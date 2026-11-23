package de.onyx4j.api.interactions.components;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Objects;
/**
 * © Copyright 23.11.2026 - 23:11 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class Components {

	private Components() {
	}

	public static JsonObject button(String customId, String label, String style) {
		if (isNonEmpty(customId) || isNonEmpty(label)) {
			throw new IllegalArgumentException("Button ID and label are required");
		}
		if (!isButtonStyle(style) || "link".equals(style)) {
			throw new IllegalArgumentException("Unsupported button style: " + style);
		}
		JsonObject button = new JsonObject();
		button.addProperty("type", "button");
		button.addProperty("custom_id", customId);
		button.addProperty("label", label);
		button.addProperty("style", style);
		return button;
	}

	public static JsonObject primary(String customId, String label) {
		return button(customId, label, "primary");
	}

	public static JsonObject secondary(String customId, String label) {
		return button(customId, label, "secondary");
	}

	public static JsonObject success(String customId, String label) {
		return button(customId, label, "success");
	}

	public static JsonObject danger(String customId, String label) {
		return button(customId, label, "danger");
	}

	public static JsonObject link(String url, String label) {
		if (isNonEmpty(url) || isNonEmpty(label)) {
			throw new IllegalArgumentException("Button URL and label are required");
		}
		JsonObject button = new JsonObject();
		button.addProperty("type", "button");
		button.addProperty("style", "link");
		button.addProperty("url", url);
		button.addProperty("label", label);
		return button;
	}

	public static JsonObject emoji(JsonObject button, String emoji) {
		JsonObject result = Objects.requireNonNull(button).deepCopy();
		result.addProperty("emoji", Objects.requireNonNull(emoji));
		return result;
	}

	public static JsonObject disabled(JsonObject component) {
		JsonObject result = Objects.requireNonNull(component).deepCopy();
		result.addProperty("disabled", true);
		return result;
	}

	public static JsonObject option(String label, String value) {
		if (isNonEmpty(label) || isNonEmpty(value)) {
			throw new IllegalArgumentException("Select option label and value are required");
		}
		JsonObject option = new JsonObject();
		option.addProperty("label", label);
		option.addProperty("value", value);
		return option;
	}

	public static JsonObject select(String customId, String placeholder, JsonObject... options) {
		if (isNonEmpty(customId) || options == null || options.length == 0 || options.length > 25) {
			throw new IllegalArgumentException("A select needs an ID and 1 to 25 options");
		}
		JsonArray entries = new JsonArray();
		for (JsonObject option : options) {
			entries.add(Objects.requireNonNull(option).deepCopy());
		}
		JsonObject select = new JsonObject();
		select.addProperty("type", "select");
		select.addProperty("custom_id", customId);
		if (placeholder != null) {
			select.addProperty("placeholder", placeholder);
		}
		select.add("options", entries);
		return select;
	}

	public static JsonObject selectUsers(String customId) {
		return entitySelect("user_select", customId);
	}

	public static JsonObject selectRoles(String customId) {
		return entitySelect("role_select", customId);
	}

	public static JsonObject selectChannels(String customId) {
		return entitySelect("channel_select", customId);
	}

	public static JsonObject row(JsonObject... components) {
		if (components == null || components.length == 0 || components.length > 5) {
			throw new IllegalArgumentException("A component row requires 1 to 5 components");
		}
		if (components.length > 1) {
			for (JsonObject component : components) {
				if (!"button".equals(componentType(component))) {
					throw new IllegalArgumentException("A row may combine buttons only");
				}
			}
		}
		JsonArray items = new JsonArray();
		for (JsonObject component : components) {
			items.add(Objects.requireNonNull(component).deepCopy());
		}
		JsonObject row = new JsonObject();
		row.addProperty("type", "row");
		row.add("items", items);
		return row;
	}

	public static JsonObject message(String content, JsonObject... rows) {
		JsonObject message = new JsonObject();
		if (content != null) {
			message.addProperty("content", content);
		}
		if (rows != null && rows.length > 0) {
			JsonArray components = new JsonArray();
			for (JsonObject row : rows) {
				if (!"row".equals(componentType(row))) {
					throw new IllegalArgumentException("Message components must contain rows");
				}
				components.add(row.deepCopy());
			}
			message.add("components", components);
		}
		if (message.isEmpty()) {
			throw new IllegalArgumentException("Message needs content or components");
		}
		return message;
	}

	private static JsonObject entitySelect(String type, String customId) {
		if (isNonEmpty(customId)) {
			throw new IllegalArgumentException("Select ID is required");
		}
		JsonObject select = new JsonObject();
		select.addProperty("type", type);
		select.addProperty("custom_id", customId);
		return select;
	}

	private static String componentType(JsonObject component) {
		if (component == null || !component.has("type")) {
			return null;
		}
		return component.get("type").getAsString();
	}

	private static boolean isButtonStyle(String style) {
		return "primary".equals(style) || "secondary".equals(style)
				|| "success".equals(style) || "danger".equals(style)
				|| "link".equals(style);
	}

	private static boolean isNonEmpty(String value) {
		return value == null || value.isBlank();
	}
}