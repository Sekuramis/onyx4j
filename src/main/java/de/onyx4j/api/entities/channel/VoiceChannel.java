package de.onyx4j.api.entities.channel;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.voice.VoiceState;
import de.onyx4j.api.entities.voice.VoiceToken;
import java.util.List;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;

import java.util.Objects;

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
public final class VoiceChannel implements ServerChannel {

    private final OnyxClient client;
    private final JsonObject data;

    public VoiceChannel(OnyxClient client, JsonObject data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data").deepCopy();
        EntityJson.encode(getId());
    }

    public VoiceChannel(OnyxClient client, String id) {
        this(client, dataWithId(id));
    }

    @Override
    public String getId() {
        return EntityJson.string(data, "id");
    }

    @Override
    public String getServerId() {
        return EntityJson.string(data, "server_id");
    }

    @Override
    public String getName() {
        return EntityJson.string(data, "name");
    }

    @Override
    public String getType() {
        return "voice";
    }

    public int getUserLimit() {
        return EntityJson.number(data, "user_limit");
    }

    public int getBitrate() {
        return EntityJson.number(data, "bitrate");
    }

    public boolean isBotVoiceAllowed() {
        return EntityJson.bool(data, "bots_voice");
    }

    public List<VoiceState> getCachedVoiceStates() {
        return client.getVoiceStateCache().getChannelStates(getId());
    }

    public RestAction<VoiceToken> requestVoiceToken() {
        return client.requestVoiceToken(getId());
    }

    public RestAction<Void> editUserLimit(int limit) {
        if (limit < 0) throw new IllegalArgumentException("User limit cannot be negative");
        JsonObject body = new JsonObject();
        body.addProperty("user_limit", limit);
        return edit(body);
    }

    public RestAction<Void> editBitrate(int bitrate) {
        if (bitrate <= 0) throw new IllegalArgumentException("Bitrate must be positive");
        JsonObject body = new JsonObject();
        body.addProperty("bitrate", bitrate);
        return edit(body);
    }

    public RestAction<Void> editBotVoiceAllowed(boolean allowed) {
        JsonObject body = new JsonObject();
        body.addProperty("bots_voice", allowed);
        return edit(body);
    }

    public RestAction<Void> edit(JsonObject changes) {
        JsonObject payload = Objects.requireNonNull(changes, "changes").deepCopy();
        return new RestAction<>(() -> client.getRestClient()
                .patch("/channels/" + EntityJson.encode(getId()), payload)
                .thenApply(result -> null));
    }

    @Override
    public RestAction<Void> editName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Channel name must not be blank");
        }
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        return new RestAction<>(() ->
                client.getRestClient()
                        .patch("/channels/" + EntityJson.encode(getId()), body)
                        .thenApply(result -> null)
        );
    }

    @Override
    public RestAction<Void> delete() {
        return new RestAction<>(() ->
                client.getRestClient()
                        .delete("/channels/" + EntityJson.encode(getId()))
                        .thenApply(result -> null)
        );
    }

    public JsonObject getRawData() {
        return data.deepCopy();
    }

    private static JsonObject dataWithId(String id) {
        JsonObject data = new JsonObject();
        EntityJson.encode(id);
        data.addProperty("id", id);
        data.addProperty("type", "voice");
        return data;
    }
}
