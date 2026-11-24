package de.onyx4j.api.interactions.components;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * © Copyright 23.11.2026 - 17:11 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class SelectMenuBuilder {

	private final String customId;
	private String placeholder;
	private boolean disabled;
	private final List<JsonObject> options = new ArrayList<>();
	private final Set<String> values = new HashSet<>();

	public SelectMenuBuilder(String customId) {
		if (customId == null || customId.isBlank())
			throw new IllegalArgumentException("Select ID is required");
		this.customId = customId;
	}

	public SelectMenuBuilder setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
		return this;
	}

	public SelectMenuBuilder setDisabled(boolean disabled) {
		this.disabled = disabled;
		return this;
	}

	public SelectMenuBuilder addOption(String label, String value) {
		JsonObject option = Components.option(label, value);
		if (options.size() >= 25)
			throw new IllegalArgumentException("At most 25 select options are supported");
		if (!values.add(value))
			throw new IllegalArgumentException("Duplicate select option: " + value);
		options.add(option);
		return this;
	}

	public JsonObject build() {
		JsonObject result = Components.select(customId, placeholder, options.toArray(JsonObject[]::new));
		return disabled ? Components.disabled(result) : result;
	}
}
