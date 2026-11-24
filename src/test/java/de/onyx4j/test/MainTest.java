package de.onyx4j.test;

import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.OnyxClientBuilder;
import de.onyx4j.api.entities.User;
import de.onyx4j.api.events.OnyxListenerAdapter;
import de.onyx4j.api.events.ReadyEvent;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.annotation.CommandOption;
import de.onyx4j.api.interactions.commands.annotation.SlashCommand;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * © Copyright 23.11.2026 - 22:29 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */

public final class MainTest extends OnyxListenerAdapter {

	private final Instant startedAt = Instant.now();

	public static void main(String[] args) throws Exception {

		MainTest handler = new MainTest();

		OnyxClient client = OnyxClientBuilder
				.createDefault(loadToken())
				.addEventListeners(handler)
				.addCommandHandlers(handler)
				.setAutoSyncCommands(true)
				.build();

		Runtime.getRuntime().addShutdownHook(
				new Thread(client::shutdown)
		);

		client.awaitReady();
	}

	@Override
	public void onReady(ReadyEvent event) {
		System.out.println("Onyx4J 0.9.0 connected!");
		System.out.println(
				"Registered commands: " +
						event.getClient()
								.getCommandRegistry()
								.getCommands()
								.size()
		);
	}

	@Override
	public void onReconnect(ReadyEvent event) {
		System.out.println("Gateway reconnected.");
	}

	@Override
	public void onDisconnect(String reason) {
		System.out.println("Disconnected: " + reason);
	}

	@Override
	public void onError(Throwable error) {
		error.printStackTrace();
	}

	@SlashCommand(
			name = "ping",
			description = "Check if the bot is online"
	)
	public void ping(SlashCommandInteractionEvent event) {
		event.reply("Pong!").queue();
	}

	@SlashCommand(
			name = "about",
			description = "Information about Onyx4J"
	)
	public void about(SlashCommandInteractionEvent event) {
		event.reply("""
                Onyx4J 0.9.0-SNAPSHOT
                The Java API for Onyx
                Java 25
                Annotation Command Framework
                Developed by Sekuramis
                """).queue();
	}

	@SlashCommand(
			name = "status",
			description = "Display the current bot status"
	)
	public void status(SlashCommandInteractionEvent event) {

		Duration uptime = Duration.between(
				startedAt,
				Instant.now()
		);

		event.reply("""
                Status: Online
                API Version: 0.9.0-SNAPSHOT
                Java Version: %s
                Uptime: %dh %dm %ds
                """.formatted(
				Runtime.version(),
				uptime.toHours(),
				uptime.toMinutesPart(),
				uptime.toSecondsPart()
		)).queue();
	}

	@SlashCommand(
			name = "whoami",
			description = "Display your user information"
	)
	public void whoami(SlashCommandInteractionEvent event) {

		User user = event.getUser();

		event.replyEphemeral("""
                Username: %s
                Display Name: %s
                User ID: %s
                """.formatted(
				user.getName(),
				user.getDisplayName(),
				user.getId()
		)).queue();
	}

	@SlashCommand(
			name = "echo",
			description = "Repeat your message"
	)
	public void echo(
			SlashCommandInteractionEvent event,

			@CommandOption(
					name = "message",
					description = "The message to repeat"
			) String message,

			@CommandOption(
					name = "ephemeral",
					description = "Only visible to you",
					required = false
			) Boolean ephemeral
	) {

		if (Boolean.TRUE.equals(ephemeral)) {
			event.replyEphemeral(message).queue();
			return;
		}

		event.reply(message).queue();
	}

	@SlashCommand(
			name = "add",
			description = "Add two numbers"
	)
	public void add(
			SlashCommandInteractionEvent event,

			@CommandOption(
					name = "first",
					description = "First number"
			) double first,

			@CommandOption(
					name = "second",
					description = "Second number"
			) double second
	) {
		event.reply(
				"Result: " + (first + second)
		).queue();
	}

	private static String loadToken() throws Exception {

		Path path = Path.of("config.yml");

		if (!Files.exists(path)) {
			throw new IllegalStateException(
					"config.yml not found"
			);
		}

		try (InputStream input = Files.newInputStream(path)) {

			Object loaded = new Yaml().load(input);

			if (!(loaded instanceof Map<?, ?> config)) {
				throw new IllegalStateException(
						"Invalid config.yml"
				);
			}

			if (!(config.get("bot") instanceof Map<?, ?> bot)) {
				throw new IllegalStateException(
						"Missing bot configuration"
				);
			}

			if (!(bot.get("token") instanceof String token)
					|| token.isBlank()) {
				throw new IllegalStateException(
						"Missing bot token"
				);
			}

			return token;
		}
	}
}