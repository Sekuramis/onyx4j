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
	private volatile Map<String, Handler> handlers = Map.of();

	public CommandRouter(CommandRegistry registry) {
		this.registry = Objects.requireNonNull(registry, "registry");
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
		if (handler == null) {
			return false;
		}
		handler.invoke(event);
		return true;
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
		return new Handler(instance, method, definition, List.copyOf(bindings));
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

	private record Handler(Object instance, Method method, SlashCommandData definition, List<Binding> bindings) {
		void invoke(SlashCommandInteractionEvent event) {
			JsonObject options = event.getOptions();
			Object[] arguments = new Object[bindings.size()];
			for (int i = 0; i < arguments.length; i++) {
				arguments[i] = bindings.get(i).resolve(event, options);
			}
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