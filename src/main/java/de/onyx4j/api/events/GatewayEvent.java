package de.onyx4j.api.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
/**
 * © Copyright 23.11.2026 - 17:10 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public class GatewayEvent {

    private final OnyxClient client;
    private final JsonObject payload;

    public GatewayEvent(OnyxClient client, JsonObject payload) {
        this.client = client;
        this.payload = payload.deepCopy();
    }

    public OnyxClient getClient() {
        return client;
    }

    public String getId() {
        return string(payload, "id");
    }

    public String getType() {
        return string(payload, "type");
    }

    public String getServerId() {
        return string(payload, "server_id");
    }

    public String getChannelId() {
        return string(payload, "channel_id");
    }

    public long getCreatedAt() {
        JsonElement element = payload.get("created_at");

        if (element == null || element.isJsonNull()) {
            return 0L;
        }

        return element.getAsLong();
    }

    public JsonObject getData() {
        JsonObject data = payload.getAsJsonObject("data");
        return data == null ? new JsonObject() : data.deepCopy();
    }

    public JsonObject getRawData() {
        return payload.deepCopy();
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);

        if (value == null || value.isJsonNull()) {
            return null;
        }

        return value.getAsString();
    }
}