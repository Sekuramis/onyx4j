package de.onyx4j.api.interactions.modals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Objects;
/**
 * © Copyright 23.11.2026 - 23:12 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ModalForm {

	private final String customId;
	private final String title;
	private final JsonArray fields = new JsonArray();
	private String submitLabel;

	private ModalForm(String customId, String title) {
		if (customId == null || customId.isBlank() || title == null || title.isBlank()) {
			throw new IllegalArgumentException("Modal ID and title are required");
		}
		this.customId = customId;
		this.title = title;
	}

	public static ModalForm create(String customId, String title) {
		return new ModalForm(customId, title);
	}

	public ModalForm submitLabel(String label) {
		this.submitLabel = Objects.requireNonNull(label);
		return this;
	}

	public ModalForm textField(String id, String label, boolean required) {
		return addField(id, label, "text", required);
	}

	public ModalForm paragraphField(String id, String label, boolean required) {
		return addField(id, label, "paragraph", required);
	}

	public ModalForm addField(String id, String label, String type, boolean required) {
		if (id == null || id.isBlank() || label == null || label.isBlank()) {
			throw new IllegalArgumentException("Field ID and label are required");
		}
		if (!"text".equals(type) && !"paragraph".equals(type)
				&& !"number".equals(type) && !"date".equals(type)
				&& !"select".equals(type) && !"checkbox".equals(type)
				&& !"file".equals(type)) {
			throw new IllegalArgumentException("Unknown modal field type: " + type);
		}
		if (fields.size() >= 10) {
			throw new IllegalArgumentException("A modal page supports up to 10 fields");
		}
		JsonObject field = new JsonObject();
		field.addProperty("id", id);
		field.addProperty("label", label);
		field.addProperty("type", type);
		field.addProperty("required", required);
		fields.add(field);
		return this;
	}

	public JsonObject toJson() {
		if (fields.isEmpty()) {
			throw new IllegalStateException("Modal must contain at least one field");
		}
		JsonObject page = new JsonObject();
		page.add("fields", fields.deepCopy());
		JsonArray pages = new JsonArray();
		pages.add(page);
		JsonObject modal = new JsonObject();
		modal.addProperty("custom_id", customId);
		modal.addProperty("title", title);
		if (submitLabel != null) {
			modal.addProperty("submit_label", submitLabel);
		}
		modal.add("pages", pages);
		return modal;
	}
}