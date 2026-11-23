package de.onyx4j.api.entities.channel;

import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.Message;
import de.onyx4j.api.requests.RestAction;

import java.util.List;

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
public interface MessageChannel {

    String getId();

    OnyxClient getClient();

    RestAction<Message> sendMessage(String content);

    RestAction<Message> retrieveMessageById(String messageId);

    RestAction<List<Message>> getHistory(int limit);

    RestAction<Void> sendTyping();
}
