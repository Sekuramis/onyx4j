package de.onyx4j.api.entities.message;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyx4j.api.interactions.components.Components;

import java.time.Instant;
import java.util.Map;
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
public final class MessageLayout {

	private static final Set<String> INNER = Set.of("text", "section", "media", "file", "separator", "actions", "columns", "progress", "countdown", "chart", "stat");
	private static final Set<String> COLUMN = Set.of("text", "stat", "progress", "countdown");

	private MessageLayout() {}

	public static JsonObject text(String text) {
		JsonObject block = block("text");
		block.addProperty("text", Objects.requireNonNull(text, "text"));
		return block;
	}

	public static JsonObject heading(String text, int level) {
		if (level < 1 || level > 3) throw new IllegalArgumentException("Heading level must be 1 to 3");
		return text("#".repeat(level) + " " + Objects.requireNonNull(text));
	}

	public static JsonObject subtext(String text) { return text("-# " + Objects.requireNonNull(text)); }

	public static JsonObject container(String accent, boolean spoiler, JsonObject... items) {
		JsonObject block = group("container", INNER, 1, 20, items);
		if (accent != null) {
			if (!accent.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Accent must be #RRGGBB");
			block.addProperty("accent", accent);
		}
		if (spoiler) block.addProperty("spoiler", true);
		return block;
	}

	public static JsonObject section(JsonObject accessory, JsonObject... texts) {
		JsonObject block = group("section", Set.of("text"), 1, 3, texts);
		if (accessory != null) {
			if (!Set.of("thumbnail", "button").contains(type(accessory)))
				throw new IllegalArgumentException("Invalid section accessory");
			block.add("accessory", accessory.deepCopy());
		}
		return block;
	}

	public static JsonObject thumbnail(String url, String alt) {
		JsonObject block = block("thumbnail");
		block.addProperty("url", Objects.requireNonNull(url));
		if (alt != null) block.addProperty("alt", alt);
		return block;
	}

	public static JsonObject media(String... urls) {
		if (urls.length < 1 || urls.length > 10) throw new IllegalArgumentException("Media requires 1 to 10 items");
		JsonArray items = new JsonArray();
		for (String url : urls) {
			JsonObject item = new JsonObject();
			item.addProperty("url", Objects.requireNonNull(url));
			items.add(item);
		}
		JsonObject block = block("media");
		block.add("items", items);
		return block;
	}

	public static JsonObject file(Attachment attachment) {
		JsonObject block = block("file");
		block.addProperty("url", attachment.url());
		block.addProperty("name", attachment.name());
		block.addProperty("size", attachment.size());
		return block;
	}

	public static JsonObject separator(boolean line, boolean large) {
		JsonObject block = block("separator");
		block.addProperty("line", line);
		block.addProperty("space", large ? "large" : "small");
		return block;
	}

	public static JsonObject actions(JsonObject... components) {
		JsonObject block = Components.row(components);
		block.addProperty("type", "actions");
		return block;
	}

	public static JsonObject columns(JsonObject... items) { return group("columns", COLUMN, 2, 4, items); }

	public static JsonObject progress(String label, double value, double max) {
		if (!Double.isFinite(value) || !Double.isFinite(max) || max <= 0)
			throw new IllegalArgumentException("Invalid progress");
		JsonObject block = block("progress");
		if (label != null) block.addProperty("label", label);
		block.addProperty("value", value);
		block.addProperty("max", max);
		return block;
	}

	public static JsonObject countdown(String label, Instant to) {
		JsonObject block = block("countdown");
		if (label != null) block.addProperty("label", label);
		block.addProperty("to", Objects.requireNonNull(to).toString());
		return block;
	}

	public static JsonObject stat(String label, String value) {
		JsonObject block = block("stat");
		block.addProperty("label", Objects.requireNonNull(label));
		block.addProperty("value", Objects.requireNonNull(value));
		return block;
	}

	public static JsonObject chart(String label, boolean line, Map<String, ? extends Number> points) {
		if (points.isEmpty() || points.size() > 24) throw new IllegalArgumentException("Chart requires 1 to 24 points");
		JsonArray entries = new JsonArray();
		points.forEach((name, value) -> {
			if (!Double.isFinite(value.doubleValue())) throw new IllegalArgumentException("Invalid chart value");
			JsonObject point = new JsonObject();
			point.addProperty("label", Objects.requireNonNull(name));
			point.addProperty("value", value);
			entries.add(point);
		});
		JsonObject block = block("chart");
		block.addProperty("kind", line ? "line" : "bar");
		if (label != null) block.addProperty("label", label);
		block.add("points", entries);
		return block;
	}

	static String type(JsonObject block) {
		return Objects.requireNonNull(block).get("type").getAsString();
	}

	static void validateTop(JsonObject block) {
		String type = type(block);
		if (!"container".equals(type) && !INNER.contains(type))
			throw new IllegalArgumentException("Invalid layout block: " + type);
	}

	static void validateLayout(JsonArray layout) {
		int[] counts = new int[2];
		for (JsonElement item : layout) count(item, counts, 0);
		if (counts[0] > 60 || counts[1] > 8000)
			throw new IllegalArgumentException("Layout exceeds 60 blocks or 8000 text characters");
	}

	private static void count(JsonElement element, int[] counts, int depth) {
		if (depth > 10) throw new IllegalArgumentException("Layout nesting too deep");
		JsonObject block = element.getAsJsonObject();
		counts[0]++;
		String type = type(block);
		if ("text".equals(type)) counts[1] += block.get("text").getAsString().length();
		if (Set.of("container", "section", "columns").contains(type)) {
			for (JsonElement item : block.getAsJsonArray("items")) count(item, counts, depth + 1);
		}
	}

	private static JsonObject block(String type) {
		JsonObject result = new JsonObject();
		result.addProperty("type", type);
		return result;
	}

	private static JsonObject group(String type, Set<String> allowed, int min, int max, JsonObject... items) {
		if (items.length < min || items.length > max) throw new IllegalArgumentException("Invalid item count: " + type);
		JsonArray array = new JsonArray();
		for (JsonObject item : items) {
			if (!allowed.contains(type(item))) throw new IllegalArgumentException("Invalid item for " + type);
			array.add(item.deepCopy());
		}
		JsonObject result = block(type);
		result.add("items", array);
		return result;
	}
}
