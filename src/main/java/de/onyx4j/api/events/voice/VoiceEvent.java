package de.onyx4j.api.events.voice;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.channel.VoiceChannel;
import de.onyx4j.api.events.GatewayEvent;
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
public abstract class VoiceEvent extends GatewayEvent {

	private final String channelId;
	private final String userId;

	protected VoiceEvent(OnyxClient client, JsonObject payload) {
		super(client, payload);
		JsonObject data = getData();
		channelId = EntityJson.string(data, "channel_id");
		userId = EntityJson.string(data, "user_id");
		EntityJson.encode(getServerId());
		EntityJson.encode(channelId);
		EntityJson.encode(userId);
	}

	@Override
	public String getChannelId() {
		return channelId;
	}

	public String getUserId() {
		return userId;
	}

	public VoiceChannel getChannel() {
		JsonObject data = new JsonObject();
		data.addProperty("id", channelId);
		data.addProperty("server_id", getServerId());
		data.addProperty("type", "voice");
		return new VoiceChannel(getClient(), data);
	}
}
