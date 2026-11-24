package de.onyx4j.api.interactions;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import de.onyx4j.api.events.interaction.AutocompleteInteractionEvent;
import de.onyx4j.api.events.interaction.ComponentInteractionEvent;
import de.onyx4j.api.events.interaction.InteractionEvent;
import de.onyx4j.api.events.interaction.ModalInteractionEvent;
import de.onyx4j.api.interactions.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Pattern;

/**
 * © Copyright 24.11.2026 - 01:06 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class InteractionRouter {

	private static final Pattern PARAM = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)}");
	private static final Pattern COMMAND = Pattern.compile("[a-z0-9_-]{1,32}");
	private static final Set<String> SELECT_TYPES = Set.of("select", "user_select", "role_select", "channel_select");
	private volatile List<Handler> handlers = List.of();

	public synchronized InteractionRouter registerHandlers(Object... instances) {
		Objects.requireNonNull(instances, "instances");
		List<Handler> next = new ArrayList<>(handlers);
		for (Object instance : instances) {
			Objects.requireNonNull(instance, "handler");
			Method[] methods = instance.getClass().getDeclaredMethods();
			Arrays.sort(methods, Comparator.comparing(Method::toGenericString));
			boolean found = false;
			for (Method method : methods) {
				if (method.isSynthetic() || method.isBridge()) continue;
				List<Route> routes = routes(method);
				if (routes.isEmpty()) continue;
				found = true;
				if (routes.size() != 1) throw invalid(method, "Exactly one interaction annotation is allowed");
				Handler handler = compile(instance, method, routes.getFirst());
				for (Handler existing : next) {
					if (handler.route.kind == existing.route.kind
							&& handler.route.specificity == existing.route.specificity
							&& handler.route.overlaps(existing.route)) {
						throw invalid(method, "Ambiguous interaction route: " + handler.route.id);
					}
				}
				next.add(handler);
			}
			if (!found) throw new IllegalArgumentException("No declared interaction handlers in " + instance.getClass().getName());
		}
		next.sort(Comparator.comparingInt((Handler handler) -> handler.route.kind == Kind.COMPONENT ? 1 : 0)
				.thenComparing(Comparator.comparingInt((Handler handler) -> handler.route.specificity).reversed()));
		handlers = List.copyOf(next);
		return this;
	}

	public boolean dispatch(ComponentInteractionEvent event) {
		Objects.requireNonNull(event, "event");
		String type = event.getComponentType();
		Kind kind = "button".equals(type) ? Kind.BUTTON
				: type != null && SELECT_TYPES.contains(type) ? Kind.SELECT : Kind.COMPONENT;
		if (route(kind, event.getCustomId(), event)) return true;
		return kind != Kind.COMPONENT && route(Kind.COMPONENT, event.getCustomId(), event);
	}

	public boolean dispatch(ModalInteractionEvent event) {
		Objects.requireNonNull(event, "event");
		return route(Kind.MODAL, event.getCustomId(), event);
	}

	public boolean dispatch(AutocompleteInteractionEvent event) {
		Objects.requireNonNull(event, "event");
		if (event.getCommandName() == null || event.getFocusedOption() == null) return false;
		return route(Kind.AUTOCOMPLETE, event.getCommandName() + ":" + event.getFocusedOption(), event);
	}

	private boolean route(Kind kind, String id, InteractionEvent event) {
		if (id == null) return false;
		for (Handler handler : handlers) {
			if (handler.route.kind != kind) continue;
			Map<String, String> values = handler.route.match(id);
			if (values == null) continue;
			handler.invoke(event, values);
			return true;
		}
		return false;
	}

	private static List<Route> routes(Method method) {
		List<Route> routes = new ArrayList<>();
		ButtonInteraction button = method.getAnnotation(ButtonInteraction.class);
		SelectInteraction select = method.getAnnotation(SelectInteraction.class);
		ComponentInteraction component = method.getAnnotation(ComponentInteraction.class);
		ModalInteraction modal = method.getAnnotation(ModalInteraction.class);
		Autocomplete autocomplete = method.getAnnotation(Autocomplete.class);
		if (button != null) routes.add(new Route(Kind.BUTTON, button.value()));
		if (select != null) routes.add(new Route(Kind.SELECT, select.value()));
		if (component != null) routes.add(new Route(Kind.COMPONENT, component.value()));
		if (modal != null) routes.add(new Route(Kind.MODAL, modal.value()));
		if (autocomplete != null) {
			if (!COMMAND.matcher(autocomplete.command()).matches() || !COMMAND.matcher(autocomplete.option()).matches())
				throw invalid(method, "Invalid autocomplete command or option name");
			routes.add(new Route(Kind.AUTOCOMPLETE, autocomplete.command() + ":" + autocomplete.option()));
		}
		return routes;
	}

	private static Handler compile(Object instance, Method method, Route route) {
		if (Modifier.isStatic(method.getModifiers()) || Modifier.isAbstract(method.getModifiers())
				|| method.isVarArgs() || method.getReturnType() != void.class || !method.trySetAccessible()) {
			throw invalid(method, "Handler must be an accessible, non-static, non-varargs void method");
		}
		Class<?> eventType = switch (route.kind) {
			case BUTTON, SELECT, COMPONENT -> ComponentInteractionEvent.class;
			case MODAL -> ModalInteractionEvent.class;
			case AUTOCOMPLETE -> AutocompleteInteractionEvent.class;
		};
		List<Binding> bindings = new ArrayList<>();
		Set<String> bound = new HashSet<>();
		boolean eventFound = false;
		for (Parameter parameter : method.getParameters()) {
			PathParam path = parameter.getAnnotation(PathParam.class);
			ModalField field = parameter.getAnnotation(ModalField.class);
			Class<?> type = parameter.getType();
			if (path == null && field == null && type == eventType && !eventFound) {
				eventFound = true;
				bindings.add(new Binding(Source.EVENT, "", type, true));
			} else if (path != null && field == null && route.names.contains(path.value())
					&& bound.add("path:" + path.value()) && supports(type, false)) {
				bindings.add(new Binding(Source.PATH, path.value(), type, true));
			} else if (field != null && path == null && route.kind == Kind.MODAL && !field.value().isBlank()
					&& bound.add("field:" + field.value()) && supports(type, true)
					&& (field.required() || !type.isPrimitive())) {
				bindings.add(new Binding(Source.FIELD, field.value(), type, field.required()));
			} else {
				throw invalid(method, "Invalid parameter: " + parameter);
			}
		}
		if (!eventFound) throw invalid(method, "Missing " + eventType.getSimpleName() + " parameter");
		return new Handler(instance, method, route, List.copyOf(bindings));
	}

	private static boolean supports(Class<?> type, boolean raw) {
		return type == String.class || type == int.class || type == Integer.class
				|| type == long.class || type == Long.class || type == double.class || type == Double.class
				|| type == boolean.class || type == Boolean.class || raw && type == JsonElement.class;
	}

	private static Object convert(JsonElement value, Class<?> type, String name, boolean required) {
		if (value == null || value.isJsonNull()) {
			if (!required) return null;
			throw new IllegalArgumentException("Missing interaction parameter: " + name);
		}
		if (type == JsonElement.class) return value.deepCopy();
		if (!value.isJsonPrimitive()) throw new IllegalArgumentException("Invalid interaction parameter: " + name);
		JsonPrimitive primitive = value.getAsJsonPrimitive();
		String text = primitive.getAsString();
		try {
			if (type == String.class && primitive.isString()) return text;
			if (type == boolean.class || type == Boolean.class) {
				if ("true".equalsIgnoreCase(text)) return true;
				if ("false".equalsIgnoreCase(text)) return false;
			} else if (type == int.class || type == Integer.class) {
				return new BigDecimal(text).intValueExact();
			} else if (type == long.class || type == Long.class) {
				return new BigDecimal(text).longValueExact();
			} else if (type == double.class || type == Double.class) {
				double number = Double.parseDouble(text);
				if (Double.isFinite(number)) return number;
			}
		} catch (ArithmeticException | NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid interaction parameter: " + name, exception);
		}
		throw new IllegalArgumentException("Invalid interaction parameter: " + name);
	}

	private static IllegalArgumentException invalid(Method method, String message) {
		return new IllegalArgumentException(method.toGenericString() + ": " + message);
	}

	private enum Kind { BUTTON, SELECT, COMPONENT, MODAL, AUTOCOMPLETE }
	private enum Source { EVENT, PATH, FIELD }

	private record Binding(Source source, String name, Class<?> type, boolean required) {
		Object resolve(InteractionEvent event, Map<String, String> path) {
			return switch (source) {
				case EVENT -> event;
				case PATH -> convert(new JsonPrimitive(path.get(name)), type, name, true);
				case FIELD -> convert(((ModalInteractionEvent) event).getRawValue(name), type, name, required);
			};
		}
	}

	private record Handler(Object instance, Method method, Route route, List<Binding> bindings) {
		void invoke(InteractionEvent event, Map<String, String> path) {
			Object[] arguments = new Object[bindings.size()];
			for (int i = 0; i < arguments.length; i++) arguments[i] = bindings.get(i).resolve(event, path);
			try {
				method.invoke(instance, arguments);
			} catch (InvocationTargetException exception) {
				Throwable cause = exception.getCause();
				if (cause instanceof Error error) throw error;
				if (cause instanceof RuntimeException runtime) throw runtime;
				throw new IllegalStateException("Interaction handler failed: " + route.id, cause);
			} catch (IllegalAccessException exception) {
				throw new IllegalStateException("Cannot invoke interaction handler: " + route.id, exception);
			}
		}
	}

	private static final class Route {
		private final Kind kind;
		private final String id;
		private final String[] parts;
		private final String[] variables;
		private final Set<String> names = new HashSet<>();
		private final int specificity;

		Route(Kind kind, String id) {
			this.kind = kind;
			this.id = id;
			if (id.isBlank()) throw new IllegalArgumentException("Interaction route must not be blank");
			parts = id.split(":", -1);
			variables = new String[parts.length];
			int literals = 0;
			for (int i = 0; i < parts.length; i++) {
				var matcher = PARAM.matcher(parts[i]);
				if (matcher.matches()) {
					String name = matcher.group(1);
					if (!names.add(name)) throw new IllegalArgumentException("Duplicate path parameter: " + name);
					variables[i] = name;
				} else {
					if (parts[i].isBlank() || parts[i].contains("{") || parts[i].contains("}"))
						throw new IllegalArgumentException("Invalid interaction route: " + id);
					literals++;
				}
			}
			specificity = literals;
		}

		Map<String, String> match(String value) {
			String[] actual = value.split(":", -1);
			if (actual.length != parts.length) return null;
			Map<String, String> captures = new HashMap<>();
			for (int i = 0; i < parts.length; i++) {
				if (variables[i] != null) {
					if (actual[i].isBlank()) return null;
					captures.put(variables[i], actual[i]);
				} else if (!parts[i].equals(actual[i])) return null;
			}
			return captures;
		}

		boolean overlaps(Route other) {
			if (parts.length != other.parts.length) return false;
			for (int i = 0; i < parts.length; i++) {
				if (variables[i] == null && other.variables[i] == null && !parts[i].equals(other.parts[i])) return false;
			}
			return true;
		}
	}
}
