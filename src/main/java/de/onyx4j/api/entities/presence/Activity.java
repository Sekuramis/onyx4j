package de.onyx4j.api.entities.presence;

import com.google.gson.JsonObject;
import java.util.Objects;

/**
 * © Copyright 23.11.2026 - 16:52 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public record Activity(Type type, String name) {

	public enum Type {
		PLAYING, LISTENING, WATCHING, CUSTOM
	}

	public Activity {
		Objects.requireNonNull(type, "type");
		if (name == null || name.isBlank() || name.codePointCount(0, name.length()) > 64)
			throw new IllegalArgumentException("Activity must contain 1 to 64 characters");
	}

	public static Activity playing(String name) { return new Activity(Type.PLAYING, name); }
	public static Activity listening(String name) { return new Activity(Type.LISTENING, name); }
	public static Activity watching(String name) { return new Activity(Type.WATCHING, name); }
	public static Activity custom(String name) { return new Activity(Type.CUSTOM, name); }

	public JsonObject toJson() {
		JsonObject data = new JsonObject();
		data.addProperty("type", type.name().toLowerCase(java.util.Locale.ROOT));
		data.addProperty("name", name);
		return data;
	}
}
