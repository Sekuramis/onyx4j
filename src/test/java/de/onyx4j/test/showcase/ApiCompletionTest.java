package de.onyx4j.test.showcase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.events.interaction.ContextCommandInteractionEvent;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.Commands;
import de.onyx4j.api.interactions.components.Components;
import de.onyx4j.api.entities.Message;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * © Copyright 23.11.2026 - 22:29 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
class ApiCompletionTest {
	private record Request(String path, String method, JsonObject body) {}

	private static final class Fixture implements AutoCloseable {
		final HttpServer server;
		final OnyxClient client;
		final LinkedBlockingQueue<Request> requests = new LinkedBlockingQueue<>();

		Fixture() throws Exception {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			server.createContext("/api/v1/", exchange -> {
				String path = exchange.getRequestURI().getPath();
				String method = exchange.getRequestMethod();
				String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				requests.add(new Request(path, method, raw.isBlank() ? new JsonObject() : JsonParser.parseString(raw).getAsJsonObject()));
				String sub = "{\"id\":\"sub1\",\"name\":\"Test\",\"events\":[\"message.created\"],\"active\":true}";
				String message = "{\"id\":\"m1\",\"channel_id\":\"c1\",\"pinned\":true}";
				String response = switch (path) {
					case "/api/v1/apps/@me/subs" -> method.equals("GET") ? "[" + sub + "]" : "{\"sub\":" + sub + ",\"secret\":\"whsec_test\"}";
					case "/api/v1/apps/@me/subs/sub1" -> sub;
					case "/api/v1/event-subs/sub1/test" -> "{\"event_id\":\"e1\"}";
					case "/api/v1/event-subs/sub1/deliveries" -> "[{\"id\":\"d1\",\"event_id\":\"e1\",\"status\":\"ok\"}]";
					case "/api/v1/event-subs/sub1/secret" -> "{\"secret\":\"whsec_new\"}";
					case "/api/v1/channels/c1/pins" -> "[" + message + "]";
					case "/api/v1/channels/c1/messages/bulk-delete" -> "{\"deleted\":2}";
					case "/api/v1/messages/m1/pin", "/api/v1/messages/m1/state" -> message;
					case "/api/v1/users/u1" -> "{\"id\":\"u1\",\"username\":\"test\"}";
					default -> "{\"ok\":true}";
				};
				byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, bytes.length);
				try (var out = exchange.getResponseBody()) { out.write(bytes); }
			});
			server.start();
			var constructor = OnyxClient.class.getDeclaredConstructor(String.class, String.class, List.class);
			constructor.setAccessible(true);
			client = constructor.newInstance("test-token", "http://127.0.0.1:" + server.getAddress().getPort(), List.of());
		}

		Request next() throws Exception {
			Request request = requests.poll(3, TimeUnit.SECONDS);
			assertNotNull(request);
			return request;
		}

		@Override
		public void close() {
			client.shutdown();
			server.stop(0);
		}
	}

	@Test
	void webhookLifecycleAndSecretRedaction() throws Exception {
		try (Fixture f = new Fixture()) {
			var created = f.client.createEventWebhook("Test", "https://example.org/events", List.of("message.created"), null).complete();
			assertEquals("POST", f.next().method());
			assertEquals("whsec_test", created.secret().value());
			assertFalse(created.toString().contains("whsec_test"));
			var webhook = created.webhook();
			assertEquals(1, f.client.retrieveEventWebhooks().complete().size());
			f.next();
			webhook.setActive(false).complete();
			assertFalse(f.next().body().get("active").getAsBoolean());
			assertEquals("e1", webhook.test().complete());
			f.next();
			assertEquals("d1", webhook.retrieveDeliveries().complete().getFirst().getId());
			f.next();
			webhook.resend("d1").complete();
			assertEquals("/api/v1/event-subs/sub1/deliveries/d1/resend", f.next().path());
			assertEquals("whsec_new", webhook.rotateSecret().complete().value());
			f.next();
			webhook.delete().complete();
			assertEquals("DELETE", f.next().method());
		}
	}

	@Test
	void messageManagementUsesOfficialRoutes() throws Exception {
		try (Fixture f = new Fixture()) {
			var channel = f.client.getTextChannelById("c1");
			Message message = channel.retrievePinnedMessages().complete().getFirst();
			assertTrue(message.isPinned());
			f.next();
			assertEquals(2, channel.bulkDeleteMessages(List.of("a", "a", "b")).complete());
			assertEquals(2, f.next().body().getAsJsonArray("ids").size());
			message.togglePin().complete();
			assertEquals("/api/v1/messages/m1/pin", f.next().path());
			JsonObject state = new JsonObject();
			state.addProperty("count", 3);
			message.setCardState(state, false).complete();
			var body = f.next().body();
			assertFalse(body.get("merge").getAsBoolean());
			assertEquals(3, body.getAsJsonObject("state").get("count").getAsInt());
			assertEquals("u1", f.client.retrieveUserById("u1").complete().getId());
			f.next();
		}
	}

	@Test
	void contextCommandsAreSeparateFromSlashCommands() throws Exception {
		try (Fixture f = new Fixture()) {
			assertEquals("user", Commands.user("Show User ID").toJson().get("type").getAsString());
			assertEquals("message", Commands.message("Show Message ID").toJson().get("type").getAsString());
			AtomicInteger contextCount = new AtomicInteger();
			AtomicInteger slashCount = new AtomicInteger();
			f.client.getEventBus().on(ContextCommandInteractionEvent.class, event -> {
				assertEquals("u1", event.getTargetUser().getId());
				contextCount.incrementAndGet();
			});
			f.client.getEventBus().on(SlashCommandInteractionEvent.class, event -> slashCount.incrementAndGet());
			JsonObject payload = JsonParser.parseString("{\"type\":\"command\",\"id\":\"i1\",\"token\":\"t1\",\"command\":{\"type\":\"user\",\"name\":\"Show User ID\",\"target\":{\"user\":{\"id\":\"u1\"}}}}").getAsJsonObject();
			var handler = OnyxClient.class.getDeclaredMethod("handleInteraction", JsonObject.class);
			handler.setAccessible(true);
			handler.invoke(f.client, payload);
			assertEquals(1, contextCount.get());
			assertEquals(0, slashCount.get());
		}
	}

	@Test
	void advancedComponentsUseNativeShapes() {
		assertTrue(Components.toggle("toggle", "Enabled", true).get("value").getAsBoolean());
		assertEquals(5, Components.slider("slider", null, 0, 10, 1, 5).get("value").getAsInt());
		assertEquals("rating", Components.rating("rating", 5, null).get("type").getAsString());
		assertThrows(IllegalArgumentException.class, () -> Components.rating("rating", 11, null));
		assertThrows(IllegalArgumentException.class, () -> Components.row(Components.toggle("t", "T", true), Components.primary("b", "B")));
	}
}
