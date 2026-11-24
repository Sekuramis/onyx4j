package de.onyx4j.api.entities.presence;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
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
public final class Presence {

	private final JsonObject data;

	private Presence(JsonObject data) {
		this.data = data.deepCopy();
	}

	public static Presence of(OnlineStatus status, Activity activity) {
		return status(status).withActivity(activity);
	}

	public static Presence status(OnlineStatus status) {
		JsonObject data = new JsonObject();
		data.addProperty("status", Objects.requireNonNull(status).getValue());
		return new Presence(data);
	}

	public static Presence activity(Activity activity) {
		return new Presence(new JsonObject()).withActivity(activity);
	}

	public Presence withActivity(Activity activity) {
		JsonObject next = toJson();
		next.add("activity", activity == null ? JsonNull.INSTANCE : activity.toJson());
		return new Presence(next);
	}

	public Presence merge(Presence update) {
		JsonObject next = toJson();
		update.data.entrySet().forEach(entry -> next.add(entry.getKey(), entry.getValue().deepCopy()));
		return new Presence(next);
	}

	public JsonObject toJson() {
		return data.deepCopy();
	}
}
