package de.onyx4j.api.concurrent;

/**
 * © Copyright 24.11.2026 - 00:53 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public record ServerTaskOptions(int partitions, int queueCapacity)
{
	public ServerTaskOptions {
		if (partitions < 1 || partitions > 64)
			throw new IllegalArgumentException("Partitions must be between 1 and 64");
		if (queueCapacity < 1 || queueCapacity > 10000)
			throw new IllegalArgumentException("Queue capacity must be between 1 and 10000");
	}

	public static ServerTaskOptions defaults() {
		return new ServerTaskOptions(4, 256);
	}
}
