package de.onyx4j.api.events.interaction;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.User;
import de.onyx4j.api.entities.Message;
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
public final class ContextCommandInteractionEvent extends InteractionEvent {
	public ContextCommandInteractionEvent(OnyxClient client, JsonObject payload) {
		super(client, payload);
		if (!"user".equals(getCommandType()) && !"message".equals(getCommandType()))
			throw new IllegalArgumentException("Invalid context command type");
	}

	public String getName() { return string(object("command"), "name"); }
	public String getCommandType() { return string(object("command"), "type"); }

	public User getTargetUser() {
		JsonObject user = EntityJson.object(EntityJson.object(object("command"), "target"), "user");
		return user == null ? null : new User(string(user, "id"), string(user, "username"), string(user, "display_name"));
	}

	public Message getTargetMessage() {
		JsonObject message = EntityJson.object(EntityJson.object(object("command"), "target"), "message");
		if (message == null) return null;
		if (!message.has("channel_id") && getChannelId() != null) message.addProperty("channel_id", getChannelId());
		return new Message(getClient(), message);
	}
}
