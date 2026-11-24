package de.onyx4j.api.entities.voice;

import java.time.Instant;
import java.util.Objects;

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
public record VoiceState(String serverId, String channelId, String userId, Instant joinedAt)
{
	public VoiceState {
		if (serverId == null || serverId.isBlank() || channelId == null || channelId.isBlank()
				|| userId == null || userId.isBlank())
			throw new IllegalArgumentException("Voice state IDs are required");
		Objects.requireNonNull(joinedAt, "joinedAt");
	}
}
