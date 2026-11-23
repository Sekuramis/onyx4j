package de.onyx4j.api.events.interaction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.Message;
import de.onyx4j.api.interactions.modals.ModalForm;
import de.onyx4j.api.requests.RestAction;

import java.util.ArrayList;
import java.util.List;

/**
 * © Copyright 23.11.2026 - 23:13 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ComponentInteractionEvent extends InteractionEvent {

	public ComponentInteractionEvent(OnyxClient client, JsonObject payload) {
		super(client, payload);
	}

	public String getCustomId() {
		return string(object("component"), "custom_id");
	}

	public String getComponentType() {
		return string(object("component"), "type");
	}

	public List<String> getValues() {
		JsonObject component = object("component");
		JsonElement element = component == null ? null : component.get("values");
		if (element == null || !element.isJsonArray()) {
			return List.of();
		}
		List<String> values = new ArrayList<>();
		for (JsonElement value : element.getAsJsonArray()) {
			values.add(value.getAsString());
		}
		return List.copyOf(values);
	}

	public JsonElement getValue() {
		JsonObject component = object("component");
		JsonElement value = component == null ? null : component.get("value");
		return value == null ? null : value.deepCopy();
	}

	public Message getMessage() {
		JsonObject message = object("message");
		if (message == null) {
			return null;
		}
		if (!message.has("channel_id") && getChannelId() != null) {
			message.addProperty("channel_id", getChannelId());
		}
		return new Message(getClient(), message);
	}

	public RestAction<Void> openModal(ModalForm modal) {
		return super.openModal(modal.toJson());
	}
}