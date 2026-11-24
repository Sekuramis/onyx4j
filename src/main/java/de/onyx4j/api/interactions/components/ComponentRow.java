package de.onyx4j.api.interactions.components;

import com.google.gson.JsonObject;

import java.util.Objects;

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
public final class ComponentRow {

	private final JsonObject data;

	private ComponentRow(JsonObject data) {
		this.data = data.deepCopy();
	}

	public static ComponentRow of(JsonObject... components) {
		return new ComponentRow(Components.row(components));
	}

	public static ComponentRow buttons(ButtonBuilder... buttons) {
		Objects.requireNonNull(buttons, "buttons");
		JsonObject[] data = new JsonObject[buttons.length];
		for (int i = 0; i < data.length; i++)
			data[i] = Objects.requireNonNull(buttons[i], "button").build();
		return of(data);
	}

	public static ComponentRow select(SelectMenuBuilder menu) {
		return of(Objects.requireNonNull(menu, "menu").build());
	}

	public JsonObject toJson() {
		return data.deepCopy();
	}
}
