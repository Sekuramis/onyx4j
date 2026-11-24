package de.onyx4j.api.events.interaction;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.User;
import de.onyx4j.api.entities.message.MessageData;
import de.onyx4j.api.requests.RestAction;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
/**
 * © Copyright 23.11.2026 - 23:13 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public abstract class InteractionEvent {

	private enum State {
		NEW, PENDING, REPLIED, DEFERRED, UPDATED, MODAL, ACKED
	}

	private final OnyxClient client;
	private final JsonObject payload;
	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
	private volatile boolean deferredEphemeral;

	protected InteractionEvent(OnyxClient client, JsonObject payload) {
		this.client = Objects.requireNonNull(client, "client");
		this.payload = Objects.requireNonNull(payload, "payload").deepCopy();
		require("id");
		require("token");
	}

	public OnyxClient getClient() {
		return client;
	}

	public String getId() {
		return require("id");
	}

	public String getToken() {
		return require("token");
	}

	public String getType() {
		return string(payload, "type");
	}

	public String getChannelId() {
		return string(payload, "channel_id");
	}

	public String getServerId() {
		return string(payload, "server_id");
	}

	public User getUser() {
		JsonObject data = object("user");
		if (data == null) {
			return null;
		}
		return new User(string(data, "id"), string(data, "username"), string(data, "display_name"));
	}

	public boolean isAcknowledged() {
		return state.get() != State.NEW;
	}

	public JsonObject getRawData() {
		return payload.deepCopy();
	}

	public RestAction<Void> reply(String message) {
		return reply(messagePayload(message), false);
	}

	public RestAction<Void> replyEphemeral(String message) {
		return reply(messagePayload(message), true);
	}

	public RestAction<Void> reply(MessageData message) {
		return reply(message.toInteractionJson());
	}

	public RestAction<Void> replyEphemeral(MessageData message) {
		return replyEphemeral(message.toInteractionJson());
	}

	public RestAction<Void> updateMessage(MessageData message) {
		return updateMessage(message.toInteractionJson());
	}

	public RestAction<Void> followup(MessageData message, boolean ephemeral) {
		return followup(message.toInteractionJson(), ephemeral);
	}

	public RestAction<Void> editOriginalReply(MessageData message) {
		return editOriginalReply(message.toInteractionJson());
	}

	public RestAction<Void> reply(JsonObject message) {
		return reply(message, false);
	}

	public RestAction<Void> replyEphemeral(JsonObject message) {
		return reply(message, true);
	}

	public RestAction<Void> deferReply() {
		return deferReply(false);
	}

	public RestAction<Void> deferReply(boolean ephemeral) {
		return new RestAction<>(() -> {
			deferredEphemeral = ephemeral;
			return initial("defer", JsonNull.INSTANCE, State.DEFERRED);
		});
	}

	public RestAction<Void> acknowledge() {
		return new RestAction<>(() -> initial("ack", null, State.ACKED));
	}

	public RestAction<Void> updateMessage(JsonObject message) {
		if (!"component".equals(getType())) {
			throw new IllegalStateException("Only component interactions may update a message");
		}
		JsonObject body = Objects.requireNonNull(message).deepCopy();
		return new RestAction<>(() -> {
			State current = state.get();
			if (current == State.NEW) {
				return initial("update", body, State.UPDATED);
			}
			if (current == State.DEFERRED || current == State.UPDATED) {
				return callback("update", body).thenApply(result -> {
					state.set(State.UPDATED);
					return null;
				});
			}
			return CompletableFuture.failedFuture(new IllegalStateException("Interaction is already acknowledged"));
		});
	}

	public RestAction<Void> openModal(JsonObject modal) {
		if ("modal".equals(getType())) {
			throw new IllegalStateException("A modal interaction cannot open another modal");
		}
		JsonObject body = Objects.requireNonNull(modal).deepCopy();
		return new RestAction<>(() -> initial("modal", body, State.MODAL));
	}

	public RestAction<Void> followup(String message) {
		return followup(messagePayload(message), false);
	}

	public RestAction<Void> followup(JsonObject message, boolean ephemeral) {
		JsonObject body = Objects.requireNonNull(message).deepCopy();
		return new RestAction<>(() -> {
			State current = state.get();
			if (current != State.DEFERRED && current != State.REPLIED && current != State.UPDATED) {
				return CompletableFuture.failedFuture(new IllegalStateException("Interaction has no usable initial acknowledgement"));
			}
			return sendFollowup(body, ephemeral || (current == State.DEFERRED && deferredEphemeral));
		});
	}

	public RestAction<Void> editOriginalReply(JsonObject message) {
		JsonObject body = Objects.requireNonNull(message).deepCopy();
		return new RestAction<>(() -> {
			if (state.get() != State.REPLIED) {
				return CompletableFuture.failedFuture(new IllegalStateException("No original reply to edit"));
			}
			return client.getRestClient().patch(endpoint("original"), body).thenApply(result -> null);
		});
	}

	protected JsonObject object(String key) {
		JsonElement element = payload.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject().deepCopy() : null;
	}

	protected static String string(JsonObject object, String key) {
		JsonElement element = object == null ? null : object.get(key);
		return element == null || element.isJsonNull() ? null : element.getAsString();
	}

	private RestAction<Void> reply(JsonObject message, boolean ephemeral) {
		JsonObject body = Objects.requireNonNull(message).deepCopy();
		return new RestAction<>(() -> {
			State current = state.get();
			if (current == State.NEW) {
				return initial(ephemeral ? "ephemeral" : "reply", body, State.REPLIED);
			}
			if (current == State.DEFERRED || current == State.REPLIED || current == State.UPDATED) {
				return sendFollowup(body, ephemeral || (current == State.DEFERRED && deferredEphemeral));
			}
			return CompletableFuture.failedFuture(new IllegalStateException("Interaction is already acknowledged"));
		});
	}

	private CompletableFuture<Void> initial(String type, JsonElement data, State nextState) {
		if (!state.compareAndSet(State.NEW, State.PENDING)) {
			return CompletableFuture.failedFuture(new IllegalStateException("Interaction is already acknowledged"));
		}
		return callback(type, data)
				.whenComplete((result, error) -> {
					if (error == null) {
						state.set(nextState);
					} else {
						state.compareAndSet(State.PENDING, State.NEW);
					}
				})
				.thenApply(result -> null);
	}

	private CompletableFuture<Void> sendFollowup(JsonObject body, boolean ephemeral) {
		body.addProperty("ephemeral", ephemeral);
		return client.getRestClient().post(endpoint("followup"), body).thenApply(result -> null);
	}

	private CompletableFuture<JsonElement> callback(String type, JsonElement data) {
		JsonObject body = new JsonObject();
		body.addProperty("type", type);
		if (data != null) {
			body.add("data", data);
		}
		return client.getRestClient().post(endpoint("callback"), body);
	}

	private String endpoint(String suffix) {
		return "/interactions/" + encode(getId()) + "/" + encode(getToken()) + "/" + suffix;
	}

	private String require(String key) {
		String value = string(payload, key);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing interaction field: " + key);
		}
		return value;
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static JsonObject messagePayload(String text) {
		JsonObject object = new JsonObject();
		object.addProperty("content", Objects.requireNonNull(text));
		return object;
	}
}
