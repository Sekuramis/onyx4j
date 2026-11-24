package de.onyx4j.api.events.interaction;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.User;
import de.onyx4j.api.entities.message.MessageData;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * © Copyright 23.11.2026 - 23:13 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public abstract class InteractionEvent {

	private enum State { NEW, REPLIED, DEFERRED, UPDATED, MODAL, ACKED, FAILED }

	private final OnyxClient client;
	private final JsonObject payload;
	private volatile State state = State.NEW;
	private volatile boolean requested;
	private boolean ephemeral;
	private CompletableFuture<Void> tail = CompletableFuture.completedFuture(null);

	protected InteractionEvent(OnyxClient client, JsonObject payload) {
		this.client = Objects.requireNonNull(client, "client");
		this.payload = Objects.requireNonNull(payload, "payload").deepCopy();
		require("id");
		require("token");
	}

	public OnyxClient getClient() { return client; }
	public String getId() { return require("id"); }
	public String getToken() { return require("token"); }
	public String getType() { return string(payload, "type"); }
	public String getChannelId() { return string(payload, "channel_id"); }
	public String getServerId() { return string(payload, "server_id"); }

	public User getUser() {
		JsonObject data = object("user");
		return data == null ? null : new User(string(data, "id"), string(data, "username"), string(data, "display_name"));
	}

	public boolean isAcknowledged() { return requested; }
	public JsonObject getRawData() { return payload.deepCopy(); }

	public RestAction<Void> reply(String message) { return reply(messagePayload(message)); }
	public RestAction<Void> replyEphemeral(String message) { return replyEphemeral(messagePayload(message)); }
	public RestAction<Void> reply(MessageData message) { return reply(message.toInteractionJson()); }
	public RestAction<Void> replyEphemeral(MessageData message) { return replyEphemeral(message.toInteractionJson()); }
	public RestAction<Void> updateMessage(MessageData message) { return updateMessage(message.toInteractionJson()); }
	public RestAction<Void> followup(MessageData message, boolean ephemeral) { return followup(message.toInteractionJson(), ephemeral); }
	public RestAction<Void> editOriginalReply(MessageData message) { return editOriginalReply(message.toInteractionJson()); }
	public RestAction<Void> reply(JsonObject message) { return reply(message, false); }
	public RestAction<Void> replyEphemeral(JsonObject message) { return reply(message, true); }

	public RestAction<Void> deferReply() { return deferReply(false); }

	public RestAction<Void> deferReply(boolean privateReply) {
		return action(() -> {
			if (state != State.NEW) return state == State.FAILED ? failed() : CompletableFuture.completedFuture(null);
			ephemeral = privateReply;
			return initial("defer", JsonNull.INSTANCE, State.DEFERRED);
		});
	}

	public RestAction<Void> acknowledge() {
		return action(() -> initial("ack", null, State.ACKED));
	}

	public RestAction<Void> updateMessage(JsonObject message) {
		if (!"component".equals(getType())) throw new IllegalStateException("Only component interactions may update a message");
		JsonObject body = Objects.requireNonNull(message).deepCopy();
		return action(() -> {
			if (state == State.NEW) return initial("update", body, State.UPDATED);
			if (state == State.DEFERRED || state == State.UPDATED)
				return callback("update", body).thenAccept(result -> state = State.UPDATED);
			return failed();
		});
	}

	public RestAction<Void> openModal(JsonObject modal) {
		if ("modal".equals(getType())) throw new IllegalStateException("A modal interaction cannot open another modal");
		JsonObject body = Objects.requireNonNull(modal).deepCopy();
		return action(() -> initial("modal", body, State.MODAL));
	}

	public RestAction<Void> followup(String message) { return followup(messagePayload(message), false); }

	public RestAction<Void> followup(JsonObject message, boolean privateReply) {
		JsonObject body = Objects.requireNonNull(message).deepCopy();
		return action(() -> canFollowup() ? sendFollowup(body, privateReply || ephemeral) : failed());
	}

	public RestAction<Void> editOriginalReply(JsonObject message) {
		JsonObject body = Objects.requireNonNull(message).deepCopy();
		return action(() -> {
			if (state == State.REPLIED)
				return client.getRestClient().patch(endpoint("original"), body).thenApply(result -> null);
			if (state == State.DEFERRED) return sendFollowup(body, ephemeral);
			return failed();
		});
	}

	protected JsonObject object(String key) {
		JsonElement element = payload.get(key);
		return element != null && element.isJsonObject() ? element.getAsJsonObject().deepCopy() : null;
	}

	protected static String string(JsonObject object, String key) {
		return EntityJson.string(object, key);
	}

	private RestAction<Void> reply(JsonObject message, boolean privateReply) {
		JsonObject body = Objects.requireNonNull(message).deepCopy();
		return action(() -> {
			if (state == State.NEW) {
				ephemeral = privateReply;
				return initial(privateReply ? "ephemeral" : "reply", body, State.REPLIED);
			}
			return canFollowup() ? sendFollowup(body, privateReply || ephemeral) : failed();
		});
	}

	private boolean canFollowup() {
		return state == State.DEFERRED || state == State.REPLIED || state == State.UPDATED;
	}

	private RestAction<Void> action(Supplier<CompletableFuture<Void>> operation) {
		if ("autocomplete".equals(getType()))
			throw new IllegalStateException("Autocomplete interactions require replyChoices");
		return new RestAction<>(() -> enqueue(operation));
	}

	private synchronized CompletableFuture<Void> enqueue(Supplier<CompletableFuture<Void>> operation) {
		requested = true;
		CompletableFuture<Void> result = tail.thenCompose(ignored -> operation.get());
		tail = result.handle((ignored, error) -> null);
		return result.copy();
	}

	private CompletableFuture<Void> initial(String type, JsonElement data, State nextState) {
		if (state != State.NEW) return failed();
		return callback(type, data).whenComplete((result, error) ->
				state = error == null ? nextState : State.FAILED).thenApply(result -> null);
	}

	private CompletableFuture<Void> sendFollowup(JsonObject body, boolean privateReply) {
		JsonObject data = body.deepCopy();
		data.addProperty("ephemeral", privateReply);
		return client.getRestClient().post(endpoint("followup"), data).thenAccept(result -> {
			if (state == State.DEFERRED) state = State.REPLIED;
		});
	}

	private CompletableFuture<JsonElement> callback(String type, JsonElement data) {
		JsonObject body = new JsonObject();
		body.addProperty("type", type);
		if (data != null) body.add("data", data);
		return client.getRestClient().post(endpoint("callback"), body);
	}

	private String endpoint(String suffix) {
		return "/interactions/" + EntityJson.encode(getId()) + "/" + EntityJson.encode(getToken()) + "/" + suffix;
	}

	private String require(String key) {
		String value = string(payload, key);
		if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing interaction field: " + key);
		return value;
	}

	private static CompletableFuture<Void> failed() {
		return CompletableFuture.failedFuture(new IllegalStateException("Interaction has no usable acknowledgement for this operation"));
	}

	private static JsonObject messagePayload(String text) {
		JsonObject object = new JsonObject();
		object.addProperty("content", Objects.requireNonNull(text));
		return object;
	}
}
