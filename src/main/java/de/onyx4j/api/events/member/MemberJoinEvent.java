package de.onyx4j.api.events.member;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.User;
import de.onyx4j.api.events.GatewayEvent;

/**
 * © Copyright 23.11.2026 - 17:12 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class MemberJoinEvent extends GatewayEvent {

    private final User user;

    public MemberJoinEvent(
            OnyxClient client,
            JsonObject payload
    ) {
        super(client, payload);

        JsonObject data = getData();
        JsonObject source = data.getAsJsonObject("user");

        this.user = source == null ? null : new User(
                value(source, "id"),
                value(source, "username"),
                value(source, "display_name")
        );
    }

    public User getUser() {
        return user;
    }

    private static String value(JsonObject object, String key) {
        JsonElement element = object.get(key);

        if (element == null || element.isJsonNull()) {
            return null;
        }

        return element.getAsString();
    }
}