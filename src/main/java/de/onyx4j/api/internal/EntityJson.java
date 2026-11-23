package de.onyx4j.api.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
public final class EntityJson {

    private EntityJson() {
    }

    public static String string(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    public static int number(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    public static long longNumber(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value == null || value.isJsonNull() ? 0L : value.getAsLong();
    }

    public static boolean bool(JsonObject object, String key) {
        JsonElement value = object == null ? null : object.get(key);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    public static JsonObject object(JsonObject parent, String key) {
        JsonElement value = parent == null ? null : parent.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    public static JsonObject requireObject(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalStateException("Onyx API returned an unexpected JSON object");
        }
        return element.getAsJsonObject();
    }

    public static JsonArray requireArray(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalStateException("Onyx API returned an unexpected JSON array");
        }
        return element.getAsJsonArray();
    }

    public static List<String> strings(JsonObject object, String key) {
        JsonElement element = object == null ? null : object.get(key);
        if (element == null || !element.isJsonArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonNull()) {
                values.add(entry.getAsString());
            }
        }
        return List.copyOf(values);
    }

    public static String encode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("An Onyx ID must not be empty");
        }
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
