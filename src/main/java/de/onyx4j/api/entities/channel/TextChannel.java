package de.onyx4j.api.entities.channel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.Message;
import de.onyx4j.api.entities.message.MessageData;
import de.onyx4j.api.entities.message.ScheduledMessage;
import de.onyx4j.api.entities.message.ScheduleRepeat;
import de.onyx4j.api.entities.panel.Panel;
import de.onyx4j.api.entities.panel.PanelPlacement;
import java.time.Instant;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
public final class TextChannel implements MessageChannel, ServerChannel {

    private final OnyxClient client;
    private final String id;
    private final JsonObject channelData;

    public TextChannel(OnyxClient client, String id) {
        this(client, onlyId(id));
    }

    public TextChannel(OnyxClient client, JsonObject channelData) {
        this.client = Objects.requireNonNull(client, "client");
        this.channelData = Objects.requireNonNull(channelData, "channelData").deepCopy();
        this.id = EntityJson.string(channelData, "id");
        EntityJson.encode(this.id);
    }

    private static JsonObject onlyId(String id) {
        EntityJson.encode(id);
        JsonObject data = new JsonObject();
        data.addProperty("id", id);
        return data;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public OnyxClient getClient() {
        return client;
    }

    @Override
    public String getServerId() {
        return EntityJson.string(channelData, "server_id");
    }

    @Override
    public String getName() {
        return EntityJson.string(channelData, "name");
    }

    @Override
    public String getType() {
        return "text";
    }

    public String getTopic() {
        return EntityJson.string(channelData, "topic");
    }

    public JsonObject getRawData() {
        return channelData.deepCopy();
    }

    @Override
    public RestAction<Void> editName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Channel name must not be blank");
        }
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        return edit(body);
    }

    public RestAction<Void> editTopic(String topic) {
        JsonObject body = new JsonObject();
        if (topic == null) {
            body.add("topic", com.google.gson.JsonNull.INSTANCE);
        } else {
            body.addProperty("topic", topic);
        }
        return edit(body);
    }

    public RestAction<Void> edit(JsonObject changes) {
        JsonObject body = Objects.requireNonNull(changes).deepCopy();
        return new RestAction<>(() -> client.getRestClient()
                .patch("/channels/" + encode(id), body)
                .thenApply(response -> null));
    }

    @Override
    public RestAction<Void> delete() {
        return new RestAction<>(() -> client.getRestClient()
                .delete("/channels/" + encode(id))
                .thenApply(response -> null));
    }

    @Override
    public RestAction<Message> sendMessage(String content) {
        Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }

        return new RestAction<>(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("content", content);
            return client.getRestClient()
                    .post("/channels/" + encode(id) + "/messages", body)
                    .thenApply(this::toMessage);
        });
    }

    @Override
    public RestAction<Message> sendMessage(MessageData messageData) {
        return sendMessage(Objects.requireNonNull(messageData, "messageData").toJson());
    }

    public RestAction<Message> sendReply(String messageId, MessageData messageData) {
        return sendMessage(Objects.requireNonNull(messageData, "messageData").withReplyTo(messageId));
    }

    public RestAction<Message> sendMessage(JsonObject messageData) {
        JsonObject payload = java.util.Objects
                .requireNonNull(messageData)
                .deepCopy();

        return new RestAction<>(() ->
                client.getRestClient()
                        .post("/channels/" + encode(id) + "/messages", payload)
                        .thenApply(this::toMessage)
        );
    }

    public RestAction<Message> sendReply(String messageId, String content) {
        Objects.requireNonNull(content, "content");
        if (content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be blank");
        }
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("Message ID is required");
        }

        return new RestAction<>(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("content", content);
            body.addProperty("reply_to", messageId);
            return client.getRestClient()
                    .post("/channels/" + encode(id) + "/messages", body)
                    .thenApply(this::toMessage);
        });
    }

    @Override
    public RestAction<Message> retrieveMessageById(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("Message ID is required");
        }

        return new RestAction<>(() ->
                client.getRestClient()
                        .get("/channels/" + encode(id) + "/messages/" + encode(messageId))
                        .thenApply(this::toMessage)
        );
    }

    @Override
    public RestAction<List<Message>> getHistory(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException("History limit must be between 1 and 100");
        }

        return new RestAction<>(() ->
                client.getRestClient()
                        .get("/channels/" + encode(id) + "/messages?limit=" + limit)
                        .thenApply(result -> {
                            JsonArray array = result.getAsJsonArray();
                            List<Message> messages = new ArrayList<>(array.size());
                            for (JsonElement item : array) {
                                messages.add(toMessage(item));
                            }
                            return List.copyOf(messages);
                        })
        );
    }

    public RestAction<List<Message>> retrievePinnedMessages() {
        return new RestAction<>(() -> client.getRestClient().get("/channels/" + encode(id) + "/pins")
                .thenApply(result -> {
                    List<Message> messages = new ArrayList<>();
                    for (JsonElement item : EntityJson.requireArray(result)) messages.add(toMessage(item));
                    return List.copyOf(messages);
                }));
    }

    public RestAction<Integer> bulkDeleteMessages(List<String> messageIds) {
        var unique = new java.util.LinkedHashSet<>(messageIds);
        if (unique.isEmpty() || unique.size() > 100)
            throw new IllegalArgumentException("Bulk delete requires 1 to 100 unique message IDs");
        JsonArray ids = new JsonArray();
        for (String messageId : unique) {
            EntityJson.encode(messageId);
            ids.add(messageId);
        }
        JsonObject body = new JsonObject();
        body.add("ids", ids);
        return new RestAction<>(() -> client.getRestClient()
                .post("/channels/" + encode(id) + "/messages/bulk-delete", body)
                .thenApply(result -> EntityJson.requireObject(result).get("deleted").getAsInt()));
    }

    public RestAction<List<Message>> getHistory(int limit, String before, String around) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("History limit must be 1 to 100");
        if (before != null && around != null) throw new IllegalArgumentException("Choose before or around");
        String path = "/channels/" + encode(id) + "/messages?limit=" + limit
                + (before == null ? "" : "&before=" + EntityJson.encode(before))
                + (around == null ? "" : "&around=" + EntityJson.encode(around));
        return new RestAction<>(() -> client.getRestClient().get(path).thenApply(result -> {
            List<Message> messages = new ArrayList<>();
            for (JsonElement item : EntityJson.requireArray(result)) messages.add(toMessage(item));
            return List.copyOf(messages);
        }));
    }

    public RestAction<List<Panel>> retrievePanels() {
        return client.retrievePanels(id);
    }

    public RestAction<Panel> createPanel(String title, String url, PanelPlacement placement, JsonObject state) {
        return client.createPanel(id, title, url, placement, state);
    }

    public RestAction<List<ScheduledMessage>> retrieveScheduledMessages() {
        return client.retrieveScheduledMessages(id);
    }

    public RestAction<ScheduledMessage> scheduleMessage(MessageData message, Instant sendAt, ScheduleRepeat repeat) {
        return client.scheduleMessage(id, message, sendAt, repeat);
    }

    @Override
    public RestAction<Void> sendTyping() {
        return new RestAction<>(() ->
                client.getRestClient()
                        .post("/channels/" + encode(id) + "/typing", new JsonObject())
                        .thenApply(result -> null)
        );
    }

    private Message toMessage(JsonElement result) {
        if (result == null || !result.isJsonObject()) {
            throw new IllegalStateException("Onyx returned an invalid message response");
        }
        JsonObject message = result.getAsJsonObject().deepCopy();
        if (!message.has("channel_id") || message.get("channel_id").isJsonNull())
            message.addProperty("channel_id", id);
        return new Message(client, message);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
