package de.onyx4j.api.interactions.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import de.onyx4j.api.requests.OnyxRestClient;
import de.onyx4j.api.requests.RestAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * © Copyright 24.11.2026 - 00:04 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class CommandRegistry {

	private final OnyxRestClient rest;
	private final Map<String, SlashCommandData> commands = new LinkedHashMap<>();
	private boolean autoSync;

	public CommandRegistry(OnyxRestClient rest) {
		this.rest = Objects.requireNonNull(rest, "rest");
	}

	public synchronized CommandRegistry addCommands(SlashCommandData... definitions) {
		Objects.requireNonNull(definitions, "definitions");

		Map<String, SlashCommandData> incoming = new LinkedHashMap<>();

		for (SlashCommandData definition : definitions) {
			SlashCommandData command = Objects.requireNonNull(definition, "command");
			String key = command.type() + ":" + command.name();

			if (commands.containsKey(key) || incoming.putIfAbsent(key, command) != null) {
				throw new IllegalArgumentException("Duplicate command: " + key);
			}
		}

		if (commands.size() + incoming.size() > 100)
			throw new IllegalArgumentException("At most 100 commands are supported");
		commands.putAll(incoming);
		return this;
	}

	public synchronized List<SlashCommandData> getCommands() {
		return List.copyOf(commands.values());
	}

	public synchronized CommandRegistry setAutoSync(boolean enabled) {
		autoSync = enabled;
		return this;
	}

	public synchronized boolean isAutoSync() {
		return autoSync;
	}

	public synchronized boolean hasCommands() {
		return !commands.isEmpty();
	}

	public RestAction<JsonElement> sync() {
		JsonArray payload = snapshot();

		if (payload.isEmpty()) {
			throw new IllegalStateException(
					"No commands registered. Refusing to replace remote commands with an empty list."
			);
		}

		return new RestAction<>(
				() -> rest.put("/apps/@me/commands", payload.deepCopy())
		);
	}

	public CompletableFuture<JsonElement> syncIfEnabled() {
		if (!isAutoSync() || !hasCommands()) {
			return CompletableFuture.completedFuture(JsonNull.INSTANCE);
		}

		return sync().queue();
	}

	private synchronized JsonArray snapshot() {
		JsonArray payload = new JsonArray();

		for (SlashCommandData command : commands.values()) {
			payload.add(command.toJson());
		}

		return payload;
	}
}
