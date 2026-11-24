package de.onyx4j.api.entities.webhook;

import com.google.gson.JsonObject;
import de.onyx4j.api.internal.EntityJson;

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
public final class WebhookDelivery {
	private final JsonObject data;

	public WebhookDelivery(JsonObject data) {
		this.data = data.deepCopy();
		EntityJson.encode(getId());
	}

	public String getId() { return EntityJson.string(data, "id"); }
	public String getEventId() { return EntityJson.string(data, "event_id"); }
	public String getEventType() { return EntityJson.string(data, "event_type"); }
	public String getStatus() { return EntityJson.string(data, "status"); }
	public int getAttempts() { return EntityJson.number(data, "attempts"); }
	public String getLastError() { return EntityJson.string(data, "last_error"); }
	public JsonObject getRawData() { return data.deepCopy(); }
}
