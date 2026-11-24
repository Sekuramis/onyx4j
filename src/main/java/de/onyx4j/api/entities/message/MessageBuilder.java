package de.onyx4j.api.entities.message;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import de.onyx4j.api.interactions.components.Components;
import de.onyx4j.api.interactions.components.ComponentRow;

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
public final class MessageBuilder {

	private String content;
	private String replyTo;
	private final List<JsonObject> rows = new ArrayList<>();
	private final JsonArray embeds = new JsonArray();
	private final JsonArray attachments = new JsonArray();
	private final JsonArray layout = new JsonArray();
	private JsonObject cardState;
	private Boolean allowEveryone;

	public MessageBuilder setContent(String content) {
		this.content = content;
		return this;
	}

	public MessageBuilder append(String text) {
		content = (content == null ? "" : content) + Objects.requireNonNull(text, "text");
		return this;
	}

	public MessageBuilder appendLine(String text) {
		return append(Objects.requireNonNull(text, "text") + "\n");
	}

	public MessageBuilder setReplyTo(String messageId) {
		if (messageId != null && messageId.isBlank())
			throw new IllegalArgumentException("Message ID is required");
		replyTo = messageId;
		return this;
	}

	public MessageBuilder addRow(ComponentRow row) {
		rows.add(Objects.requireNonNull(row, "row").toJson());
		return this;
	}

	public MessageBuilder addRow(JsonObject... components) {
		rows.add(Components.row(components));
		return this;
	}

	public MessageBuilder clearRows() {
		rows.clear();
		return this;
	}

	public MessageBuilder addEmbed(EmbedBuilder embed) {
		embeds.add(Objects.requireNonNull(embed).build());
		return this;
	}

	public MessageBuilder addAttachment(Attachment attachment) {
		attachments.add(Objects.requireNonNull(attachment).toJson());
		return this;
	}

	public MessageBuilder addLayout(JsonObject... blocks) {
		if (layout.size() + blocks.length > 10) throw new IllegalArgumentException("At most 10 top-level layout blocks");
		for (JsonObject block : blocks) MessageLayout.validateTop(block);
		for (JsonObject block : blocks) layout.add(block.deepCopy());
		return this;
	}

	public MessageBuilder setCardState(JsonObject state) {
		cardState = state == null ? null : state.deepCopy();
		return this;
	}

	public MessageBuilder setAllowEveryoneMention(boolean allowed) {
		allowEveryone = allowed;
		return this;
	}

	public MessageData build() {
		String text = content == null || content.isBlank() ? null : content;
		JsonObject payload = text != null || !rows.isEmpty()
				? Components.message(text, rows.toArray(JsonObject[]::new)) : new JsonObject();
		if (!embeds.isEmpty()) payload.add("embeds", embeds.deepCopy());
		if (!attachments.isEmpty()) payload.add("attachments", attachments.deepCopy());
		if (!layout.isEmpty()) {
			MessageLayout.validateLayout(layout);
			payload.add("layout", layout.deepCopy());
		}
		if (payload.isEmpty()) throw new IllegalArgumentException("Message cannot be empty");
		if (cardState != null) payload.add("card_state", cardState.deepCopy());
		if (allowEveryone != null) {
			JsonObject mentions = new JsonObject();
			mentions.addProperty("everyone", allowEveryone);
			payload.add("allowed_mentions", mentions);
		}
		if (replyTo != null) payload.addProperty("reply_to", replyTo);
		return new MessageData(payload);
	}
}
