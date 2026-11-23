package de.onyx4j.api.interactions.commands;

/**
 * © Copyright 23.11.2026 - 23:47 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public enum CommandOptionType {

	STRING("string"),
	INTEGER("integer"),
	NUMBER("number"),
	BOOLEAN("boolean"),
	USER("user"),
	ROLE("role"),
	CHANNEL("channel"),
	ATTACHMENT("attachment"),
	DATE("date");

	private final String value;

	CommandOptionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}