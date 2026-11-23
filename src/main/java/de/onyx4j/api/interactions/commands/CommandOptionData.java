package de.onyx4j.api.interactions.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
/**
 * © Copyright 23.11.2026 - 23:48 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class CommandOptionData {

	private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9_-]{1,32}");

	private final String name;
	private final String description;
	private final CommandOptionType type;
	private final List<CommandOptionChoice> choices = new ArrayList<>();

	private boolean required;
	private boolean autocomplete;
	private Double min;
	private Double max;

	public CommandOptionData(String name, String description, CommandOptionType type) {
		if (name == null || !NAME_PATTERN.matcher(name).matches()) {
			throw new IllegalArgumentException("Option name must match [a-z0-9_-]{1,32}");
		}
		if (description == null || description.isBlank()) {
			throw new IllegalArgumentException("Option description must not be blank");
		}

		this.name = name;
		this.description = description;
		this.type = Objects.requireNonNull(type, "type");
	}

	public static CommandOptionData of(String name, String description, CommandOptionType type) {
		return new CommandOptionData(name, description, type);
	}

	public static CommandOptionData string(String name, String description) {
		return of(name, description, CommandOptionType.STRING);
	}

	public static CommandOptionData integer(String name, String description) {
		return of(name, description, CommandOptionType.INTEGER);
	}

	public static CommandOptionData bool(String name, String description) {
		return of(name, description, CommandOptionType.BOOLEAN);
	}

	public CommandOptionData setRequired(boolean required) {
		this.required = required;
		return this;
	}

	public CommandOptionData setAutocomplete(boolean autocomplete) {
		if (autocomplete && !choices.isEmpty()) {
			throw new IllegalStateException(
					"Autocomplete and static choices are mutually exclusive"
			);
		}
		this.autocomplete = autocomplete;
		return this;
	}

	public CommandOptionData addChoices(CommandOptionChoice... values) {
		Objects.requireNonNull(values, "values");

		if (autocomplete) {
			throw new IllegalStateException(
					"Autocomplete and static choices are mutually exclusive"
			);
		}

		if (choices.size() + values.length > 25) {
			throw new IllegalArgumentException("At most 25 static choices are supported");
		}

		for (CommandOptionChoice choice : values) {
			Objects.requireNonNull(choice, "choice");

			if (type == CommandOptionType.STRING && !choice.value().isString()) {
				throw new IllegalArgumentException("String options require string choices");
			}

			if ((type == CommandOptionType.INTEGER || type == CommandOptionType.NUMBER)
					&& !choice.value().isNumber()) {
				throw new IllegalArgumentException("Numeric options require numeric choices");
			}

			if (type != CommandOptionType.STRING
					&& type != CommandOptionType.INTEGER
					&& type != CommandOptionType.NUMBER) {
				throw new IllegalStateException(
						"Choices require a string or numeric option"
				);
			}

			choices.add(choice);
		}

		return this;
	}

	public CommandOptionData setMin(double min) {
		validateNumericBound(min);

		if (max != null && min > max) {
			throw new IllegalArgumentException("Minimum exceeds maximum");
		}

		this.min = min;
		return this;
	}

	public CommandOptionData setMax(double max) {
		validateNumericBound(max);

		if (min != null && max < min) {
			throw new IllegalArgumentException("Maximum is below minimum");
		}

		this.max = max;
		return this;
	}

	public String getName() {
		return name;
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();

		object.addProperty("name", name);
		object.addProperty("description", description);
		object.addProperty("type", type.getValue());
		object.addProperty("required", required);

		if (autocomplete) {
			object.addProperty("autocomplete", true);
		}

		if (min != null) {
			addBound(object, "min", min);
		}

		if (max != null) {
			addBound(object, "max", max);
		}

		if (!choices.isEmpty()) {
			JsonArray array = new JsonArray();

			for (CommandOptionChoice choice : choices) {
				array.add(choice.toJson());
			}

			object.add("choices", array);
		}

		return object;
	}

	private void validateNumericBound(double bound) {
		if (type != CommandOptionType.INTEGER && type != CommandOptionType.NUMBER) {
			throw new IllegalStateException(
					"Bounds are only supported on numeric options"
			);
		}

		if (!Double.isFinite(bound)) {
			throw new IllegalArgumentException("Numeric bounds must be finite");
		}

		if (type == CommandOptionType.INTEGER && bound != Math.rint(bound)) {
			throw new IllegalArgumentException(
					"Integer bounds must be whole numbers"
			);
		}
	}

	private void addBound(JsonObject object, String key, double bound) {
		if (type == CommandOptionType.INTEGER) {
			if (bound < Long.MIN_VALUE || bound > Long.MAX_VALUE) {
				throw new IllegalArgumentException("Integer bound is out of range");
			}
			object.addProperty(key, (long) bound);
		} else {
			object.addProperty(key, bound);
		}
	}
}