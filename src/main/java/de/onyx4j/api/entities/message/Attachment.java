package de.onyx4j.api.entities.message;

import com.google.gson.JsonObject;
import de.onyx4j.api.internal.EntityJson;

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
public record Attachment(String url, String name, long size, String type, Long expiresAt, boolean expired) {

	public Attachment {
		if (url == null || url.isBlank() || name == null || name.isBlank() || type == null || type.isBlank() || size < 0)
			throw new IllegalArgumentException("Invalid attachment");
	}

	public static Attachment fromJson(JsonObject data) {
		return new Attachment(EntityJson.string(data, "url"), EntityJson.string(data, "name"),
				EntityJson.longNumber(data, "size"), EntityJson.string(data, "type"),
				data.has("expires_at") && !data.get("expires_at").isJsonNull() ? data.get("expires_at").getAsLong() : null,
				EntityJson.bool(data, "expired"));
	}

	public JsonObject toJson() {
		JsonObject data = new JsonObject();
		data.addProperty("url", url);
		data.addProperty("name", name);
		data.addProperty("size", size);
		data.addProperty("type", type);
		return data;
	}
}
