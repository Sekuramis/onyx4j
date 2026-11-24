package de.onyx4j.api.entities.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

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
public final class EmbedBuilder {

	private final JsonObject data = new JsonObject();

	public EmbedBuilder setTitle(String title) { return property("title", title); }
	public EmbedBuilder setDescription(String description) { return property("description", description); }
	public EmbedBuilder setUrl(String url) { return property("url", url); }

	public EmbedBuilder setColor(String color) {
		if (color != null && !color.matches("#[0-9a-fA-F]{6}"))
			throw new IllegalArgumentException("Color must be #RRGGBB");
		return property("color", color);
	}

	public EmbedBuilder setColor(int rgb) {
		if (rgb < 0 || rgb > 0xffffff) throw new IllegalArgumentException("Invalid RGB color");
		return setColor("#%06x".formatted(rgb));
	}

	public EmbedBuilder setAuthor(String name, String url, String iconUrl) {
		JsonObject author = new JsonObject();
		author.addProperty("name", Objects.requireNonNull(name, "name"));
		if (url != null) author.addProperty("url", url);
		if (iconUrl != null) author.addProperty("icon_url", iconUrl);
		data.add("author", author);
		return this;
	}

	public EmbedBuilder setFooter(String text) { return setFooter(text, null); }

	public EmbedBuilder setFooter(String text, String iconUrl) {
		JsonObject footer = new JsonObject();
		footer.addProperty("text", Objects.requireNonNull(text, "text"));
		if (iconUrl != null) footer.addProperty("icon_url", iconUrl);
		data.add("footer", footer);
		return this;
	}

	public EmbedBuilder setImage(String url) { return image("image", url); }
	public EmbedBuilder setThumbnail(String url) { return image("thumbnail", url); }

	public EmbedBuilder setTimestamp(Instant timestamp) {
		return property("timestamp", timestamp == null ? null : timestamp.toString());
	}

	public EmbedBuilder addField(String name, String value, boolean inline) {
		JsonObject field = new JsonObject();
		field.addProperty("name", Objects.requireNonNull(name, "name"));
		field.addProperty("value", Objects.requireNonNull(value, "value"));
		if (inline) field.addProperty("inline", true);
		array("fields").add(field);
		return this;
	}

	public EmbedBuilder addBlock(JsonObject block) {
		if (!Set.of("progress", "countdown", "chart", "stat").contains(MessageLayout.type(block)))
			throw new IllegalArgumentException("Invalid embed value block");
		array("blocks").add(block.deepCopy());
		return this;
	}

	public JsonObject build() {
		if (data.isEmpty()) throw new IllegalArgumentException("Embed cannot be empty");
		return data.deepCopy();
	}

	private EmbedBuilder property(String key, String value) {
		if (value == null) data.remove(key);
		else data.addProperty(key, value);
		return this;
	}

	private EmbedBuilder image(String key, String url) {
		if (url == null) data.remove(key);
		else {
			JsonObject image = new JsonObject();
			image.addProperty("url", url);
			data.add(key, image);
		}
		return this;
	}

	private JsonArray array(String key) {
		if (!data.has(key)) data.add(key, new JsonArray());
		return data.getAsJsonArray(key);
	}
}
