package de.onyx4j.api.interactions.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Objects;
/**
 * © Copyright 23.11.2026 - 23:48 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public record CommandOptionChoice(String name, JsonPrimitive value) {

	public CommandOptionChoice {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Choice name must not be blank");
		}
		Objects.requireNonNull(value, "value");
		if (!value.isString() && !value.isNumber()) {
			throw new IllegalArgumentException("Choice value must be a string or number");
		}
	}

	public static CommandOptionChoice of(String name, String value) {
		return new CommandOptionChoice(
				name,
				new JsonPrimitive(Objects.requireNonNull(value))
		);
	}

	public static CommandOptionChoice of(String name, long value) {
		return new CommandOptionChoice(name, new JsonPrimitive(value));
	}

	public static CommandOptionChoice of(String name, double value) {
		if (!Double.isFinite(value)) {
			throw new IllegalArgumentException("Choice number must be finite");
		}
		return new CommandOptionChoice(name, new JsonPrimitive(value));
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();
		object.addProperty("name", name);
		object.add("value", value.deepCopy());
		return object;
	}
}