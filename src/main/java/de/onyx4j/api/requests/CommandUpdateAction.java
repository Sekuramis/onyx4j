package de.onyx4j.api.requests;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.interactions.commands.SlashCommandData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
public final class CommandUpdateAction {

    private final OnyxClient jda;
    private final List<SlashCommandData> commands = new ArrayList<>();

    public CommandUpdateAction(OnyxClient jda) {
        this.jda = jda;
    }

    public CommandUpdateAction addCommands(
            SlashCommandData... commands
    ) {
        this.commands.addAll(Arrays.asList(commands));
        return this;
    }

    public RestAction<JsonElement> submit() {
        return new RestAction<>(() -> {
            JsonArray payload = new JsonArray();

            for (SlashCommandData command : commands) {
                payload.add(command.toJson());
            }

            return jda.getRestClient()
                    .put("/apps/@me/commands", payload);
        });
    }

    public void queue() {
        submit().queue();
    }

    public JsonElement complete() {
        return submit().complete();
    }
}