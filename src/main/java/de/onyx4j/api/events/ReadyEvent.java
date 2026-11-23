package de.onyx4j.api.events;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;

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
public final class ReadyEvent {

    private final OnyxClient client;
    private final boolean reconnect;
    private final JsonObject data;

    public ReadyEvent(
            OnyxClient client,
            boolean reconnect,
            JsonObject data
    ) {
        this.client = client;
        this.reconnect = reconnect;
        this.data = data.deepCopy();
    }

    public OnyxClient getClient() {
        return client;
    }

    public boolean isReconnect() {
        return reconnect;
    }

    public JsonObject getData() {
        return data.deepCopy();
    }
}