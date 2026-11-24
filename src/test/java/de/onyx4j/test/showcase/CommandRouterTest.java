package de.onyx4j.test.showcase;

import com.google.gson.JsonObject;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.CommandRegistry;
import de.onyx4j.api.interactions.commands.CommandRouter;
import de.onyx4j.api.interactions.commands.SlashCommandData;
import de.onyx4j.api.interactions.commands.annotation.CommandOption;
import de.onyx4j.api.interactions.commands.annotation.SlashCommand;
import de.onyx4j.api.requests.OnyxRestClient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 * © Copyright 24.11.2026 - 01:07 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
class CommandRouterTest {

	private final CommandRegistry registry = new CommandRegistry(new OnyxRestClient("test-token", "http://localhost:9999"));
	private final CommandRouter router = new CommandRouter(registry);

	@Test
	void registersDefinitionsAndDispatchesTypedArguments() {
		Example handler = new Example();
		router.registerHandlers(handler);
		var definition = registry.getCommands().getFirst();
		assertEquals("echo", definition.name());
		var options = definition.toJson().getAsJsonArray("options");
		assertEquals("message", options.get(0).getAsJsonObject().get("name").getAsString());
		assertEquals("count", options.get(1).getAsJsonObject().get("name").getAsString());
		assertTrue(router.dispatch(event("echo", values("Hello", 3))));
		assertEquals("Hello", handler.message);
		assertEquals(3L, handler.count.longValue());
		assertTrue(router.dispatch(event("echo", values("Again", null))));
		assertNull(handler.count);
		assertFalse(router.dispatch(event("unknown", new JsonObject())));
	}

	@Test
	void rejectsDuplicateAndInvalidRegistrationsAtomically() {
		router.registerHandlers(new Example());
		assertThrows(IllegalArgumentException.class, () -> router.registerHandlers(new Example()));
		assertThrows(IllegalArgumentException.class, () -> router.registerHandlers(new Other(), new Invalid()));
		assertFalse(router.hasHandler("other"));
		assertEquals(1, registry.getCommands().size());
		registry.addCommands(new SlashCommandData("other", "Manual"));
		assertThrows(IllegalArgumentException.class, () -> router.registerHandlers(new Other()));
		assertFalse(router.hasHandler("other"));
	}

	@Test
	void rejectsMissingOrMalformedOptionsAndPropagatesHandlerErrors() {
		router.registerHandlers(new Example(), new Broken());
		assertThrows(IllegalArgumentException.class, () -> router.dispatch(event("echo", new JsonObject())));
		JsonObject invalid = values("Hello", null);
		invalid.addProperty("count", 1.5);
		assertThrows(IllegalArgumentException.class, () -> router.dispatch(event("echo", invalid)));
		invalid.addProperty("count", "3");
		assertThrows(IllegalArgumentException.class, () -> router.dispatch(event("echo", invalid)));
		assertThrows(IllegalStateException.class, () -> router.dispatch(event("broken", new JsonObject())));
	}

	private static JsonObject values(String message, Integer count) {
		JsonObject values = new JsonObject();
		values.addProperty("message", message);
		if (count != null) values.addProperty("count", count);
		return values;
	}

	private static SlashCommandInteractionEvent event(String name, JsonObject options) {
		JsonObject command = new JsonObject();
		command.addProperty("name", name);
		command.add("options", options);
		JsonObject payload = new JsonObject();
		payload.add("command", command);
		return new SlashCommandInteractionEvent(null, payload);
	}

	private static final class Example {
		private String message;
		private Long count;

		@SlashCommand(name = "echo", description = "Echo a message")
		private void echo(@CommandOption(name = "count", description = "Count", required = false) Long count,
				SlashCommandInteractionEvent event,
				@CommandOption(name = "message", description = "Message") String message) {
			this.message = message;
			this.count = count;
		}
	}

	private static final class Other {
		@SlashCommand(name = "other", description = "Other")
		void other(SlashCommandInteractionEvent event) {
		}
	}

	private static final class Invalid {
		@SlashCommand(name = "invalid", description = "Invalid")
		void invalid(SlashCommandInteractionEvent event,
				@CommandOption(name = "count", description = "Count", required = false) int count) {
		}
	}

	private static final class Broken {
		@SlashCommand(name = "broken", description = "Broken")
		void broken(SlashCommandInteractionEvent event) {
			throw new IllegalStateException("Test");
		}
	}
}