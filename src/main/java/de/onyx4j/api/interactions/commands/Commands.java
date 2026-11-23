package de.onyx4j.api.interactions.commands;

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
public final class Commands {

    private Commands() {
    }

    public static SlashCommandData slash(
            String name,
            String description
    ) {
        if (!name.matches("[a-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException(
                    "Invalid slash command name: " + name
            );
        }

        return new SlashCommandData(name, description, "slash");
    }
}