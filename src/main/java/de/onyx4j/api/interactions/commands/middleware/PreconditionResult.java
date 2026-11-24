package de.onyx4j.api.interactions.commands.middleware;

/**
 * © Copyright 24.11.2026 - 01:06 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public record PreconditionResult(boolean allowed, String message)
{
	public PreconditionResult {
		if (!allowed && (message == null || message.isBlank()))
			throw new IllegalArgumentException("A denied precondition requires a message");
	}

	public static PreconditionResult allow() {
		return new PreconditionResult(true, null);
	}

	public static PreconditionResult deny(String message) {
		return new PreconditionResult(false, message);
	}
}
