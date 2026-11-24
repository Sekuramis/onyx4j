package de.onyx4j.api.entities.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;
import java.time.Instant;
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
public final class ScheduledMessage {

	private final OnyxClient client;
	private final JsonObject data;

	public ScheduledMessage(OnyxClient client, JsonObject data) {
		this.client = Objects.requireNonNull(client);
		this.data = Objects.requireNonNull(data).deepCopy();
		EntityJson.encode(getId());
	}

	public String getId() { return EntityJson.string(data, "id"); }
	public String getChannelId() { return EntityJson.string(data, "channel_id"); }
	public String getContent() { return EntityJson.string(data, "content"); }
	public Instant getSendAt() { return Instant.ofEpochMilli(EntityJson.longNumber(data, "send_at")); }
	public String getRepeat() { return EntityJson.string(data, "repeat"); }
	public boolean isActive() { return EntityJson.bool(data, "active"); }
	public JsonArray getEmbeds() {
		var value = data.get("embeds");
		return value != null && value.isJsonArray() ? value.getAsJsonArray().deepCopy() : new JsonArray();
	}
	public JsonObject getRawData() { return data.deepCopy(); }

	public RestAction<Void> delete() {
		return new RestAction<>(() -> client.getRestClient()
				.delete("/scheduled/" + EntityJson.encode(getId())).thenApply(result -> null));
	}
}
