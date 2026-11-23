package de.onyx4j.api.events.interaction;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.interactions.commands.CommandOptionChoice;
import de.onyx4j.api.requests.RestAction;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * © Copyright 23.11.2026 - 23:49 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class AutocompleteInteractionEvent extends InteractionEvent {

	private final AtomicBoolean acknowledged = new AtomicBoolean(false);

	public AutocompleteInteractionEvent(OnyxClient client, JsonObject payload) {
		super(client, payload);

		if (!"autocomplete".equals(getType())) {
			throw new IllegalArgumentException(
					"Expected an autocomplete interaction"
			);
		}
	}

	public String getCommandName() {
		return string(object("command"), "name");
	}

	public String getFocusedOption() {
		return string(object("autocomplete"), "option");
	}

	public String getFocusedValue() {
		String value = string(object("autocomplete"), "value");
		return value == null ? "" : value;
	}

	public JsonObject getOptions() {
		JsonObject autocomplete = object("autocomplete");

		if (autocomplete == null) {
			return new JsonObject();
		}

		JsonElement options = autocomplete.get("options");

		return options != null && options.isJsonObject()
				? options.getAsJsonObject().deepCopy()
				: new JsonObject();
	}

	public String getOption(String name) {
		JsonElement element = getOptions().get(name);

		return element == null || element.isJsonNull()
				? null
				: element.getAsString();
	}

	@Override
	public boolean isAcknowledged() {
		return acknowledged.get();
	}

	public RestAction<Void> replyChoices(CommandOptionChoice... choices) {
		Objects.requireNonNull(choices, "choices");
		return replyChoices(Arrays.asList(choices));
	}

	public RestAction<Void> replyChoices(List<CommandOptionChoice> choices) {
		Objects.requireNonNull(choices, "choices");

		if (choices.size() > 25) {
			throw new IllegalArgumentException(
					"At most 25 autocomplete choices are supported"
			);
		}

		JsonArray array = new JsonArray();

		for (CommandOptionChoice choice : choices) {
			array.add(Objects.requireNonNull(choice, "choice").toJson());
		}

		return new RestAction<>(() -> {
			if (!acknowledged.compareAndSet(false, true)) {
				return CompletableFuture.failedFuture(
						new IllegalStateException(
								"Autocomplete interaction is already acknowledged"
						)
				);
			}

			JsonObject callback = new JsonObject();
			callback.addProperty("type", "choices");
			callback.add("data", array.deepCopy());

			String route = "/interactions/"
					+ encode(getId())
					+ "/"
					+ encode(getToken())
					+ "/callback";

			return getClient()
					.getRestClient()
					.post(route, callback)
					.thenApply(result -> null);
		});
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}