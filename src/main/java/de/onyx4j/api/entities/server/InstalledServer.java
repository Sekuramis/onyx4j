package de.onyx4j.api.entities.server;

import com.google.gson.JsonObject;
import de.onyx4j.api.internal.EntityJson;

/**
 * © Copyright 24.11.2026 - 00:53 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public record InstalledServer(String id, String name, String icon, long memberCount)
{
	public InstalledServer {
		if (id == null || id.isBlank())
			throw new IllegalArgumentException("Server ID is required");
		if (memberCount < 0)
			throw new IllegalArgumentException("Member count cannot be negative");
	}

	public static InstalledServer fromJson(JsonObject data) {
		return new InstalledServer(EntityJson.string(data, "id"),
				EntityJson.string(data, "name"), EntityJson.string(data, "icon"),
				EntityJson.longNumber(data, "member_count"));
	}
}
