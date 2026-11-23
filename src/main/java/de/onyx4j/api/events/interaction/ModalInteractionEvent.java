package de.onyx4j.api.events.interaction;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
/**
 * © Copyright 23.11.2026 - 23:14 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ModalInteractionEvent extends InteractionEvent {

	public ModalInteractionEvent(OnyxClient client, JsonObject payload) {
		super(client, payload);
	}

	public String getCustomId() {
		return string(object("form"), "custom_id");
	}

	public JsonObject getValues() {
		JsonObject form = object("form");
		JsonElement values = form == null ? null : form.get("values");
		return values != null && values.isJsonObject() ? values.getAsJsonObject().deepCopy() : new JsonObject();
	}

	public String getValue(String fieldId) {
		return string(getValues(), fieldId);
	}

	public JsonElement getRawValue(String fieldId) {
		JsonElement element = getValues().get(fieldId);
		return element == null ? null : element.deepCopy();
	}
}