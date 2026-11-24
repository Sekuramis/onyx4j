package de.onyx4j.api.entities.message;

import de.onyx4j.api.entities.Message;
import java.util.Objects;

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
public record DirectMessage(String channelId, Message message) {
	public DirectMessage {
		if (channelId == null || channelId.isBlank()) throw new IllegalArgumentException("Channel ID is required");
		Objects.requireNonNull(message, "message");
	}
}
