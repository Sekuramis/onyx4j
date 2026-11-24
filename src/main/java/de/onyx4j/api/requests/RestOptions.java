package de.onyx4j.api.requests;

import java.time.Duration;
import java.util.Objects;

/**
 * © Copyright 23.11.2026 - 16:51 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public record RestOptions(Duration connectTimeout, Duration requestTimeout, int maxRetries)
{
	public RestOptions {
		Objects.requireNonNull(connectTimeout, "connectTimeout");
		Objects.requireNonNull(requestTimeout, "requestTimeout");
		if (connectTimeout.isNegative() || connectTimeout.isZero()
				|| requestTimeout.isNegative() || requestTimeout.isZero())
			throw new IllegalArgumentException("Timeouts must be positive");
		if (maxRetries < 0 || maxRetries > 10)
			throw new IllegalArgumentException("Retries must be between 0 and 10");
	}

	public static RestOptions defaults() {
		return new RestOptions(Duration.ofSeconds(15), Duration.ofSeconds(15), 3);
	}
}
