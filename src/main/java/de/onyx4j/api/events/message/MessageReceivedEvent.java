package de.onyx4j.api.events.message;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.Message;
import de.onyx4j.api.entities.channel.TextChannel;
import de.onyx4j.api.events.GatewayEvent;

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
public final class MessageReceivedEvent extends GatewayEvent {

    private final Message message;

    public MessageReceivedEvent(
            OnyxClient client,
            JsonObject payload
    ) {
        super(client, payload);
        JsonObject data = getData();
        if ((!data.has("channel_id") || data.get("channel_id").isJsonNull())
                && getChannelId() != null) {
            data.addProperty("channel_id", getChannelId());
        }
        this.message = new Message(client, data);
    }

    public Message getMessage() {
        return message;
    }

    public String getMessageId() {
        return message.getId();
    }

    public TextChannel getChannel() {
        return getClient().getTextChannelById(message.getChannelId());
    }
}