package de.onyx4j.api.entities.panel;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;
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
public final class Panel {

	private final OnyxClient client;
	private final JsonObject data;

	public Panel(OnyxClient client, JsonObject data) {
		this.client = Objects.requireNonNull(client);
		this.data = Objects.requireNonNull(data).deepCopy();
		EntityJson.encode(getId());
	}

	public String getId() { return EntityJson.string(data, "id"); }
	public String getServerId() { return EntityJson.string(data, "server_id"); }
	public String getChannelId() { return EntityJson.string(data, "channel_id"); }
	public String getAppId() { return EntityJson.string(data, "app_id"); }
	public String getTitle() { return EntityJson.string(data, "title"); }
	public String getUrl() { return EntityJson.string(data, "url"); }
	public String getPlacement() { return EntityJson.string(data, "placement"); }
	public JsonObject getState() {
		JsonObject state = EntityJson.object(data, "state");
		return state == null ? new JsonObject() : state.deepCopy();
	}
	public JsonObject getRawData() { return data.deepCopy(); }

	public RestAction<Panel> edit(String title, String url, JsonObject state) {
		JsonObject body = new JsonObject();
		if (title != null) body.addProperty("title", title);
		if (url != null) body.addProperty("url", url);
		if (state != null) body.add("state", state.deepCopy());
		if (body.isEmpty()) throw new IllegalArgumentException("Panel update is empty");
		return new RestAction<>(() -> client.getRestClient().patch(path(), body)
				.thenApply(result -> new Panel(client, EntityJson.requireObject(result))));
	}

	public RestAction<JsonObject> setState(JsonObject state) {
		JsonObject body = new JsonObject();
		body.add("set", Objects.requireNonNull(state).deepCopy());
		return new RestAction<>(() -> client.getRestClient().patch(path() + "/state", body)
				.thenApply(result -> EntityJson.requireObject(EntityJson.requireObject(result).get("state")).deepCopy()));
	}

	public RestAction<Void> delete() {
		return new RestAction<>(() -> client.getRestClient().delete(path()).thenApply(result -> null));
	}

	private String path() { return "/panels/" + EntityJson.encode(getId()); }
}
