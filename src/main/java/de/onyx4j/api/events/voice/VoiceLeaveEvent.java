package de.onyx4j.api.events.voice;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import java.time.Duration;

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
public final class VoiceLeaveEvent extends VoiceEvent {

	private final long durationMillis;

	public VoiceLeaveEvent(OnyxClient client, JsonObject payload) {
		super(client, payload);
		var value = getData().get("duration_ms");
		if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
			throw new IllegalArgumentException("Voice duration is required");
		durationMillis = value.getAsBigDecimal().longValueExact();
		if (durationMillis < 0) throw new IllegalArgumentException("Voice duration cannot be negative");
	}

	public long getDurationMillis() {
		return durationMillis;
	}

	public Duration getDuration() {
		return Duration.ofMillis(durationMillis);
	}
}
