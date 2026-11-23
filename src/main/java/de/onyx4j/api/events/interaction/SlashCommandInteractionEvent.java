package de.onyx4j.api.events.interaction;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.requests.RestAction;
import de.onyx4j.api.entities.User;

import java.util.concurrent.CompletableFuture;
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
public final class SlashCommandInteractionEvent {

    private final OnyxClient jda;
    private final JsonObject payload;
    private CompletableFuture<Void> pending =
            CompletableFuture.completedFuture(null);

    private boolean responded;
    private boolean ephemeral;

    public SlashCommandInteractionEvent(
            OnyxClient jda,
            JsonObject payload
    ) {
        this.jda = jda;
        this.payload = payload;
    }

    public String getName() {
        return payload.getAsJsonObject("command")
                .get("name").getAsString();
    }

    public String getId() {
        return payload.get("id").getAsString();
    }

    public String getToken() {
        return payload.get("token").getAsString();
    }

    public User getUser() {
        JsonObject user = payload.getAsJsonObject("user");

        return new User(
                value(user, "id"),
                value(user, "username"),
                value(user, "display_name")
        );
    }

    public JsonObject getOptions() {
        JsonObject command = payload.getAsJsonObject("command");

        if (command.has("options")
                && command.get("options").isJsonObject()) {
            return command.getAsJsonObject("options");
        }

        return new JsonObject();
    }

    public String getOption(String name) {
        JsonObject options = getOptions();

        if (!options.has(name) || options.get(name).isJsonNull()) {
            return null;
        }

        return options.get(name).getAsString();
    }

    public RestAction<Void> reply(String message) {
        return respond("reply", message);
    }

    public RestAction<Void> replyEphemeral(String message) {
        return respond("ephemeral", message);
    }

    public RestAction<Void> deferReply() {
        return new RestAction<>(() -> {
            synchronized (this) {
                if (responded) {
                    return CompletableFuture.failedFuture(
                            new IllegalStateException("Already acknowledged")
                    );
                }

                responded = true;

                JsonObject body = new JsonObject();
                body.addProperty("type", "defer");

                return enqueue(callbackPath(), body);
            }
        });
    }

    private RestAction<Void> respond(String type, String message) {
        return new RestAction<>(() -> {
            synchronized (this) {
                JsonObject body = new JsonObject();
                JsonObject data = new JsonObject();
                data.addProperty("content", message);

                String path;

                if (!responded) {
                    responded = true;
                    ephemeral = type.equals("ephemeral");
                    body.addProperty("type", type);
                    body.add("data", data);
                    path = callbackPath();
                } else {
                    data.addProperty(
                            "ephemeral",
                            ephemeral || type.equals("ephemeral")
                    );
                    body = data;
                    path = followupPath();
                }

                return enqueue(path, body);
            }
        });
    }

    private CompletableFuture<Void> enqueue(
            String path,
            JsonObject body
    ) {
        CompletableFuture<Void> result = pending.thenCompose(
                unused -> jda.getRestClient()
                        .post(path, body)
                        .thenApply(response -> null)
        );

        pending = result.handle((unused, error) -> null);

        return result;
    }

    private String callbackPath() {
        return "/interactions/" + getId()
                + "/" + getToken() + "/callback";
    }

    private String followupPath() {
        return "/interactions/" + getId()
                + "/" + getToken() + "/followup";
    }

    private static String value(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return null;
        }

        return obj.get(key).getAsString();
    }
}