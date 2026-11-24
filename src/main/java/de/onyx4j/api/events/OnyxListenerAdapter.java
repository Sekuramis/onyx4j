package de.onyx4j.api.events;

import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.events.interaction.ContextCommandInteractionEvent;
import de.onyx4j.api.events.member.MemberJoinEvent;
import de.onyx4j.api.events.voice.VoiceJoinEvent;
import de.onyx4j.api.events.voice.VoiceLeaveEvent;
import de.onyx4j.api.events.message.MessageReceivedEvent;
import de.onyx4j.api.events.interaction.ComponentInteractionEvent;
import de.onyx4j.api.events.interaction.ModalInteractionEvent;
import de.onyx4j.api.events.interaction.AutocompleteInteractionEvent;



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

    public void onContextCommandInteraction(ContextCommandInteractionEvent event) {
    }

    public void onVoiceJoin(VoiceJoinEvent event) {
    }

    public void onVoiceLeave(VoiceLeaveEvent event) {
    }

    public void onDisconnect(String reason) {
    }

    public void onError(Throwable error) {
    }

    public void onComponentInteraction(ComponentInteractionEvent event) {
    }

    public void onModalInteraction(ModalInteractionEvent event) {
    }

    public void onAutocompleteInteraction(AutocompleteInteractionEvent event) {
    }

    public void onReconnect(ReadyEvent event) {
    }

}
