package de.onyx4j.api.interactions.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.annotation.CommandOption;
import de.onyx4j.api.interactions.commands.annotation.SlashCommand;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import de.onyx4j.api.interactions.commands.middleware.CommandMiddleware;
import de.onyx4j.api.interactions.commands.middleware.CommandExceptionHandler;
import de.onyx4j.api.interactions.commands.middleware.PreconditionResult;
/**
 * © Copyright 24.11.2026 - 01:06 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class CommandRouter {

	private final CommandRegistry registry;
	private final Consumer<Throwable> errorHandler;
	private volatile Map<String, Handler> handlers = Map.of();
	private volatile Set<String> botOwnerIds = Set.of();
	private volatile CommandExceptionHandler exceptionHandler;

	public CommandRouter(CommandRegistry registry) {
		this(registry, Throwable::printStackTrace);
	}

	public CommandRouter(CommandRegistry registry, Consumer<Throwable> errorHandler) {
		this.registry = Objects.requireNonNull(registry, "registry");
		this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
	}

	public CommandRouter setBotOwnerIds(String... ids) {
		Objects.requireNonNull(ids, "ids");
		for (String id : ids) {
			if (id == null || id.isBlank()) throw new IllegalArgumentException("Bot owner ID must not be blank");
		}
		botOwnerIds = Set.copyOf(Arrays.asList(ids));
		return this;
	}

	public CommandRouter setExceptionHandler(CommandExceptionHandler handler) {
		exceptionHandler = Objects.requireNonNull(handler, "handler");
		return this;
	}

	public synchronized CommandRouter registerHandlers(Object... instances) {
		Objects.requireNonNull(instances, "instances");
		Map<String, Handler> next = new LinkedHashMap<>(handlers);
		List<SlashCommandData> definitions = new ArrayList<>();

		for (Object instance : instances) {
			Objects.requireNonNull(instance, "handler");
			if (instance instanceof Class<?>) {
				throw new IllegalArgumentException("Pass a command handler instance, not a class");
			}
			Method[] methods = instance.getClass().getDeclaredMethods();
			Arrays.sort(methods, Comparator.comparing(Method::toGenericString));
			boolean found = false;
			for (Method method : methods) {
				if (method.isBridge() || method.isSynthetic()) {
					continue;
				}
				SlashCommand annotation = method.getAnnotation(SlashCommand.class);
				if (annotation == null) {
					continue;
				}
				found = true;
				Handler handler = compile(instance, method, annotation);
				if (next.putIfAbsent(annotation.name(), handler) != null) {
					throw new IllegalArgumentException("Duplicate slash command: " + annotation.name());
				}
				definitions.add(handler.definition());
			}
			if (!found) {
				throw new IllegalArgumentException("No declared @SlashCommand methods in " + instance.getClass().getName());
			}
		}

		registry.addCommands(definitions.toArray(SlashCommandData[]::new));
		handlers = Map.copyOf(next);
		return this;
	}

	public boolean hasHandler(String name) {
		return name != null && handlers.containsKey(name);
	}

	public boolean dispatch(SlashCommandInteractionEvent event) {
		Objects.requireNonNull(event, "event");
		Handler handler = handlers.get(event.getName());
		if (handler == null) return false;

		try {
			PreconditionResult owner = handler.middleware().checkOwner(event, botOwnerIds);
			if (!owner.allowed()) {
				deny(event, owner.message());
				return true;
			}
		} catch (RuntimeException exception) {
			report(event, exception, true);
			return true;
		}

		if (!handler.access().isProtected()) {
			execute(handler, event, true);
			return true;
		}

		try {
			handler.access().authorize(event).whenComplete((allowed, error) -> {
				if (error != null) {
					deny(event, "Unable to verify your permissions.");
					errorHandler.accept(error);
					return;
				}
				if (!Boolean.TRUE.equals(allowed)) {
					deny(event, "You do not have permission to use this command here.");
					return;
				}
				execute(handler, event, false);
			}).exceptionally(error -> {
				errorHandler.accept(error);
				return null;
			});
		} catch (RuntimeException exception) {
			deny(event, "Unable to verify your permissions.");
			errorHandler.accept(exception);
		}
		return true;
	}

	private void execute(Handler handler, SlashCommandInteractionEvent event, boolean synchronous) {
		try {
			Object[] arguments = handler.resolve(event);
			PreconditionResult check = handler.middleware().check(event);
			if (!check.allowed()) {
				deny(event, check.message());
				return;
			}
			PreconditionResult cooldown = handler.middleware().acquire(event);
			if (!cooldown.allowed()) {
				deny(event, cooldown.message());
				return;
			}
			handler.invoke(arguments);
		} catch (RuntimeException exception) {
			report(event, exception, synchronous);
		}
	}

	private void report(SlashCommandInteractionEvent event, RuntimeException error, boolean synchronous) {
		CommandExceptionHandler handler = exceptionHandler;
		if (handler == null) {
			if (synchronous) throw error;
			errorHandler.accept(error);
			return;
		}
		errorHandler.accept(error);
		try {
			handler.onException(event, error);
		} catch (RuntimeException exception) {
			errorHandler.accept(exception);
		}
	}

	private void deny(SlashCommandInteractionEvent event, String message) {
		event.replyEphemeral(message).queue(result -> {}, errorHandler);
	}

	private static Handler compile(Object instance, Method method, SlashCommand annotation) {
		if (Modifier.isStatic(method.getModifiers()) || Modifier.isAbstract(method.getModifiers())
				|| method.getReturnType() != void.class || method.isVarArgs()) {
			throw invalid(method, "Command methods must be non-static, non-abstract, non-varargs and return void");
		}
		if (!method.trySetAccessible()) {
			throw invalid(method, "Command method is not accessible");
		}

		SlashCommandData definition = new SlashCommandData(annotation.name(), annotation.description());
		List<Binding> bindings = new ArrayList<>();
		List<CommandOptionData> required = new ArrayList<>();
		List<CommandOptionData> optional = new ArrayList<>();
		Set<String> names = new HashSet<>();
		boolean hasEvent = false;

		for (Parameter parameter : method.getParameters()) {
			Class<?> type = parameter.getType();
			CommandOption option = parameter.getAnnotation(CommandOption.class);
			if (type == SlashCommandInteractionEvent.class) {
				if (option != null || hasEvent) {
					throw invalid(method, "Only one unannotated event parameter is allowed");
				}
				hasEvent = true;
				bindings.add(new Binding(null, type, true));
				continue;
			}
			if (option == null) {
				throw invalid(method, "Every non-event parameter requires @CommandOption");
			}
			if (!names.add(option.name())) {
				throw invalid(method, "Duplicate option: " + option.name());
			}
			if (!option.required() && type.isPrimitive()) {
				throw invalid(method, "Optional options require a nullable boxed type");
			}
			CommandOptionType optionType = optionType(type, method);
			if (option.autocomplete() && optionType != CommandOptionType.STRING
					&& optionType != CommandOptionType.INTEGER && optionType != CommandOptionType.NUMBER) {
				throw invalid(method, "Autocomplete requires a string or numeric option");
			}
			CommandOptionData data = CommandOptionData.of(option.name(), option.description(), optionType)
					.setRequired(option.required()).setAutocomplete(option.autocomplete());
			(option.required() ? required : optional).add(data);
			bindings.add(new Binding(option.name(), type, option.required()));
		}
		if (!hasEvent) {
			throw invalid(method, "A SlashCommandInteractionEvent parameter is required");
		}
		required.addAll(optional);
		definition = definition.addOptions(required.toArray(CommandOptionData[]::new));
		return new Handler(instance, method, definition, List.copyOf(bindings), CommandAccess.from(method), CommandMiddleware.from(method));
	}

	private static CommandOptionType optionType(Class<?> type, Method method) {
		if (type == String.class) return CommandOptionType.STRING;
		if (type == int.class || type == Integer.class || type == long.class || type == Long.class)
			return CommandOptionType.INTEGER;
		if (type == double.class || type == Double.class) return CommandOptionType.NUMBER;
		if (type == boolean.class || type == Boolean.class) return CommandOptionType.BOOLEAN;
		throw invalid(method, "Unsupported option type: " + type.getTypeName());
	}

	private static IllegalArgumentException invalid(Method method, String message) {
		return new IllegalArgumentException(method.toGenericString() + ": " + message);
	}

	private record Binding(String name, Class<?> type, boolean required) {
		Object resolve(SlashCommandInteractionEvent event, JsonObject options) {
			if (name == null) return event;
			JsonElement value = options.get(name);
			if (value == null || value.isJsonNull()) {
				if (!required) return null;
				throw new IllegalArgumentException("Missing required option: " + name);
			}
			if (!value.isJsonPrimitive()) {
				throw new IllegalArgumentException("Invalid option value: " + name);
			}
			JsonPrimitive primitive = value.getAsJsonPrimitive();
			try {
				if (type == String.class && primitive.isString()) return primitive.getAsString();
				if ((type == boolean.class || type == Boolean.class) && primitive.isBoolean())
					return primitive.getAsBoolean();
				if (primitive.isNumber()) {
					if (type == int.class || type == Integer.class) return primitive.getAsBigDecimal().intValueExact();
					if (type == long.class || type == Long.class) return primitive.getAsBigDecimal().longValueExact();
					if (type == double.class || type == Double.class) {
						double number = primitive.getAsDouble();
						if (Double.isFinite(number)) return number;
					}
				}
			} catch (ArithmeticException | NumberFormatException exception) {
				throw new IllegalArgumentException("Invalid numeric option: " + name, exception);
			}
			throw new IllegalArgumentException("Invalid option type or range: " + name);
		}
	}

	private record Handler(Object instance, Method method, SlashCommandData definition, List<Binding> bindings, CommandAccess access, CommandMiddleware middleware) {
		Object[] resolve(SlashCommandInteractionEvent event) {
			JsonObject options = event.getOptions();
			Object[] arguments = new Object[bindings.size()];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = bindings.get(i).resolve(event, options);
			}
			return arguments;
		}

		void invoke(Object[] arguments) {
			try {
				method.invoke(instance, arguments);
			} catch (InvocationTargetException exception) {
				Throwable cause = exception.getCause();
				if (cause instanceof Error error) throw error;
				if (cause instanceof RuntimeException runtime) throw runtime;
				throw new IllegalStateException("Command execution failed: " + definition.name(), cause);
			} catch (IllegalAccessException exception) {
				throw new IllegalStateException("Cannot invoke command: " + definition.name(), exception);
			}
		}
	}
}
