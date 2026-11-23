package de.onyx4j.api.events;

import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.events.member.MemberJoinEvent;
import de.onyx4j.api.events.message.MessageReceivedEvent;

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
public abstract class OnyxListenerAdapter {

    public void onReady(ReadyEvent event) {
    }

    public void onGatewayEvent(GatewayEvent event) {
    }

    public void onMessageReceived(MessageReceivedEvent event) {
    }

    public void onMemberJoin(MemberJoinEvent event) {
    }

    public void onSlashCommandInteraction(
            SlashCommandInteractionEvent event
    ) {
    }

    public void onDisconnect(String reason) {
    }

    public void onError(Throwable error) {
    }
}