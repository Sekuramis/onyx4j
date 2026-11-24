package de.onyx4j.api.entities.webhook;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
public final class EventWebhook {
	private final OnyxClient client;
	private final JsonObject data;

	public EventWebhook(OnyxClient client, JsonObject data) {
		this.client = Objects.requireNonNull(client);
		this.data = data.deepCopy();
		EntityJson.encode(getId());
	}

	public String getId() { return EntityJson.string(data, "id"); }
	public String getName() { return EntityJson.string(data, "name"); }
	public String getUrl() { return EntityJson.string(data, "url"); }
	public List<String> getEvents() { return EntityJson.strings(data, "events"); }
	public List<String> getChannelIds() { return EntityJson.strings(data, "channel_ids"); }
	public boolean isActive() { return EntityJson.bool(data, "active"); }
	public boolean hasSecret() { return EntityJson.bool(data, "has_secret"); }
	public int getFailCount() { return EntityJson.number(data, "fail_count"); }
	public JsonObject getRawData() { return data.deepCopy(); }

	public RestAction<EventWebhook> edit(JsonObject changes) {
		JsonObject body = Objects.requireNonNull(changes).deepCopy();
		if (body.isEmpty() || body.keySet().stream().anyMatch(key ->
				!java.util.Set.of("name", "url", "events", "channel_ids", "active").contains(key)))
			throw new IllegalArgumentException("Invalid webhook update");
		return new RestAction<>(() -> client.getRestClient().patch(appPath(), body)
				.thenApply(result -> new EventWebhook(client, EntityJson.requireObject(result))));
	}

	public RestAction<EventWebhook> setActive(boolean active) {
		JsonObject body = new JsonObject();
		body.addProperty("active", active);
		return edit(body);
	}

	public RestAction<Void> delete() {
		return new RestAction<>(() -> client.getRestClient().delete(appPath()).thenApply(result -> null));
	}

	public RestAction<String> test() {
		return new RestAction<>(() -> client.getRestClient().post(eventPath() + "/test", new JsonObject())
				.thenApply(result -> EntityJson.string(EntityJson.requireObject(result), "event_id")));
	}

	public RestAction<List<WebhookDelivery>> retrieveDeliveries() {
		return new RestAction<>(() -> client.getRestClient().get(eventPath() + "/deliveries").thenApply(result -> {
			var deliveries = new ArrayList<WebhookDelivery>();
			for (var entry : EntityJson.requireArray(result))
				deliveries.add(new WebhookDelivery(EntityJson.requireObject(entry)));
			return List.copyOf(deliveries);
		}));
	}

	public RestAction<Void> resend(String deliveryId) {
		String path = eventPath() + "/deliveries/" + EntityJson.encode(deliveryId) + "/resend";
		return new RestAction<>(() -> client.getRestClient().post(path, new JsonObject()).thenApply(result -> null));
	}

	public RestAction<WebhookSecret> rotateSecret() {
		return new RestAction<>(() -> client.getRestClient().post(eventPath() + "/secret", new JsonObject())
				.thenApply(result -> new WebhookSecret(EntityJson.string(EntityJson.requireObject(result), "secret"))));
	}

	private String appPath() { return "/apps/@me/subs/" + EntityJson.encode(getId()); }
	private String eventPath() { return "/event-subs/" + EntityJson.encode(getId()); }
}
