package de.onyx4j.api.entities.message;

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
public final class MessageData {

	private final JsonObject data;

	MessageData(JsonObject data) {
		this.data = Objects.requireNonNull(data, "data").deepCopy();
	}

	public JsonObject toJson() {
		return data.deepCopy();
	}

	public String getContent() {
		return data.has("content") ? data.get("content").getAsString() : null;
	}

	public String getReplyTo() {
		return data.has("reply_to") ? data.get("reply_to").getAsString() : null;
	}

	public MessageData withReplyTo(String messageId) {
		if (messageId == null || messageId.isBlank())
			throw new IllegalArgumentException("Message ID is required");
		JsonObject copy = toJson();
		copy.addProperty("reply_to", messageId);
		return new MessageData(copy);
	}

	public JsonObject toInteractionJson() {
		if (getReplyTo() != null)
			throw new IllegalArgumentException("Message references are only supported for channel sends");
		return toJson();
	}
}
