package de.onyx4j.api.entities.server;

import com.google.gson.JsonObject;
import de.onyx4j.api.entities.User;
import de.onyx4j.api.internal.EntityJson;
import java.time.Instant;

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
public record ServerBan(User user, String reason, Instant createdAt) {
	public static ServerBan fromJson(JsonObject data) {
		JsonObject user = EntityJson.requireObject(data.get("user"));
		return new ServerBan(new User(EntityJson.string(user, "id"), EntityJson.string(user, "username"),
				EntityJson.string(user, "display_name")), EntityJson.string(data, "reason"),
				Instant.ofEpochMilli(EntityJson.longNumber(data, "created_at")));
	}
}
