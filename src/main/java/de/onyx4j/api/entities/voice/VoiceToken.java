package de.onyx4j.api.entities.voice;

import com.google.gson.JsonObject;
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
public final class VoiceToken {

	private final String url;
	private final String token;
	private final boolean listen;
	private final int bitrate;
	private final String region;

	public VoiceToken(JsonObject data) {
		url = EntityJson.string(data, "url");
		token = EntityJson.string(data, "token");
		if (url == null || url.isBlank() || token == null || token.isBlank())
			throw new IllegalArgumentException("Invalid voice token response");
		listen = EntityJson.bool(data, "listen");
		bitrate = EntityJson.number(data, "bitrate");
		region = EntityJson.string(data, "region");
	}

	public String getUrl() {
		return url;
	}

	public String getToken() {
		return token;
	}

	public boolean canListen() {
		return listen;
	}

	public int getBitrate() {
		return bitrate;
	}

	public String getRegion() {
		return region;
	}

	@Override
	public String toString() {
		return "VoiceToken[token=REDACTED, listen=" + listen + ", bitrate=" + bitrate + "]";
	}
}
