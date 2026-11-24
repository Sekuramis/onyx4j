package de.onyx4j.api.entities;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.channel.TextChannel;
import de.onyx4j.api.entities.message.MessageData;
import de.onyx4j.api.requests.RestAction;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
public final class Message {

    private final OnyxClient client;
    private final JsonObject data;

    public Message(JsonObject data) {
        this(null, data);
    }

    public Message(OnyxClient client, JsonObject data) {
        this.client = client;
        this.data = Objects.requireNonNull(data, "data").deepCopy();
    }

    public String getId() {
        return string(data, "id");
    }

    public String getChannelId() {
        return string(data, "channel_id");
    }

    public String getContentRaw() {
        return string(data, "content");
    }

    public String getContentDisplay() {
        return getContentRaw();
    }

    public User getAuthor() {
        JsonObject author = data.getAsJsonObject("author");

        if (author == null) {
            return null;
        }

        return new User(
                string(author, "id"),
                string(author, "username"),
                string(author, "display_name")
        );
    }

    public boolean isAuthorBot() {
        JsonObject author = data.getAsJsonObject("author");

        return author != null
                && author.has("is_bot")
                && author.get("is_bot").getAsBoolean();
    }

    public boolean isContentHidden() {
        return data.has("content_hidden")
                && data.get("content_hidden").getAsBoolean();
    }

    public TextChannel getChannel() {
        String channelId = getChannelId();
        if (channelId == null || channelId.isBlank()) {
            throw new IllegalStateException("Message has no channel ID");
        }
        return requireClient().getTextChannelById(channelId);
    }

    public RestAction<Message> reply(String content) {
        return getChannel().sendReply(requireMessageId(), content);
    }

    public RestAction<Message> reply(MessageData messageData) {
        return getChannel().sendReply(requireMessageId(), messageData);
    }

    public JsonArray getComponents() {
        JsonElement components = data.get("components");
        return components != null && components.isJsonArray()
                ? components.getAsJsonArray().deepCopy() : new JsonArray();
    }

    public RestAction<Message> editMessage(String content) {
        Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }
        OnyxClient connectedClient = requireClient();
        String messageId = requireMessageId();

        return new RestAction<>(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("content", content);
            return connectedClient.getRestClient()
                    .patch("/messages/" + encode(messageId), body)
                    .thenApply(result -> new Message(connectedClient, result.getAsJsonObject()));
        });
    }

    public RestAction<Void> delete() {
        OnyxClient connectedClient = requireClient();
        String messageId = requireMessageId();

        return new RestAction<>(() ->
                connectedClient.getRestClient()
                        .delete("/messages/" + encode(messageId))
                        .thenApply(result -> null)
        );
    }

    public RestAction<Message> addReaction(String emoji) {
        OnyxClient connectedClient = requireClient();
        String messageId = requireMessageId();
        Objects.requireNonNull(emoji, "emoji");

        return new RestAction<>(() ->
                connectedClient.getRestClient()
                        .put("/messages/" + encode(messageId) + "/reactions/" + encode(emoji), new JsonObject())
                        .thenApply(result -> new Message(connectedClient, result.getAsJsonObject()))
        );
    }

    public RestAction<Message> removeReaction(String emoji) {
        OnyxClient connectedClient = requireClient();
        String messageId = requireMessageId();
        Objects.requireNonNull(emoji, "emoji");

        return new RestAction<>(() ->
                connectedClient.getRestClient()
                        .delete("/messages/" + encode(messageId) + "/reactions/" + encode(emoji))
                        .thenApply(result -> new Message(connectedClient, result.getAsJsonObject()))
        );
    }

    public JsonObject getRawData() {
        return data.deepCopy();
    }

    private OnyxClient requireClient() {
        if (client == null) {
            throw new IllegalStateException("Message is not attached to an OnyxClient");
        }
        return client;
    }

    private String requireMessageId() {
        String id = getId();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Message has no ID");
        }
        return id;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);

        if (value == null || value.isJsonNull()) {
            return null;
        }

        return value.getAsString();
    }
}
