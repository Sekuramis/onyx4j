package de.onyx4j.api.interactions.commands.middleware;

import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.annotation.BotOwnerOnly;
import de.onyx4j.api.interactions.commands.annotation.Cooldown;
import de.onyx4j.api.interactions.commands.annotation.RequirePreconditions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * © Copyright 24.11.2026 - 01:06 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class CommandMiddleware {

	private static final int MAX_COOLDOWNS = 100_000;
	private final boolean ownerOnly;
	private final List<CommandPrecondition> preconditions;
	private final CooldownScope scope;
	private final long duration;
	private final LongSupplier clock;
	private final Map<String, Long> deadlines = new HashMap<>();
	private int acquisitions;

	private CommandMiddleware(boolean ownerOnly, List<CommandPrecondition> preconditions,
			CooldownScope scope, long duration, LongSupplier clock) {
		this.ownerOnly = ownerOnly;
		this.preconditions = List.copyOf(preconditions);
		this.scope = scope;
		this.duration = duration;
		this.clock = Objects.requireNonNull(clock, "clock");
	}

	public static CommandMiddleware from(Method method) {
		return from(method, System::nanoTime);
	}

	public static CommandMiddleware from(Method method, LongSupplier clock) {
		Cooldown cooldown = method.getAnnotation(Cooldown.class);
		RequirePreconditions annotation = method.getAnnotation(RequirePreconditions.class);
		List<CommandPrecondition> checks = new ArrayList<>();
		if (annotation != null) {
			if (annotation.value().length == 0)
				throw new IllegalArgumentException("Empty precondition list: " + method);
			for (Class<? extends CommandPrecondition> type : annotation.value()) {
				try {
					var constructor = type.getDeclaredConstructor();
					if (!constructor.trySetAccessible())
						throw new IllegalArgumentException("Precondition constructor is inaccessible: " + type.getName());
					checks.add(constructor.newInstance());
				} catch (ReflectiveOperationException exception) {
					throw new IllegalArgumentException("Precondition requires a no-argument constructor: " + type.getName(), exception);
				}
			}
		}
		if (cooldown != null && (cooldown.seconds() < 1 || cooldown.seconds() > 2_592_000))
			throw new IllegalArgumentException("Cooldown must be between 1 second and 30 days: " + method);
		return new CommandMiddleware(method.isAnnotationPresent(BotOwnerOnly.class), checks,
				cooldown == null ? CooldownScope.USER : cooldown.scope(),
				cooldown == null ? 0 : TimeUnit.SECONDS.toNanos(cooldown.seconds()), clock);
	}

	public PreconditionResult checkOwner(SlashCommandInteractionEvent event, Set<String> ownerIds) {
		if (!ownerOnly) return PreconditionResult.allow();
		String id = event.getUser().getId();
		return id != null && ownerIds.contains(id)
				? PreconditionResult.allow() : PreconditionResult.deny("Only bot owners can use this command.");
	}

	public PreconditionResult check(SlashCommandInteractionEvent event) {
		for (CommandPrecondition check : preconditions) {
			PreconditionResult result = Objects.requireNonNull(check.check(event), "Precondition returned null");
			if (!result.allowed()) return result;
		}
		return PreconditionResult.allow();
	}

	public PreconditionResult acquire(SlashCommandInteractionEvent event) {
		if (duration == 0) return PreconditionResult.allow();
		String key = switch (scope) {
			case USER -> event.getUser().getId();
			case SERVER -> event.getServerId();
			case CHANNEL -> event.getChannelId();
			case GLOBAL -> "global";
		};
		if (key == null || key.isBlank())
			return PreconditionResult.deny("This command is not available in this context.");
		return acquireKey(key);
	}

	private synchronized PreconditionResult acquireKey(String key) {
		long now = clock.getAsLong();
		if (++acquisitions % 256 == 0 || deadlines.size() >= MAX_COOLDOWNS)
			deadlines.values().removeIf(deadline -> deadline - now <= 0);
		Long deadline = deadlines.get(key);
		if (deadline != null && deadline - now > 0) {
			long remaining = 1 + (deadline - now - 1) / 1_000_000_000L;
			return PreconditionResult.deny("Please wait " + remaining + " second(s) before using this command again.");
		}
		if (!deadlines.containsKey(key) && deadlines.size() >= MAX_COOLDOWNS)
			return PreconditionResult.deny("Command cooldown capacity reached. Please try again later.");
		deadlines.put(key, now + duration);
		return PreconditionResult.allow();
	}
}
