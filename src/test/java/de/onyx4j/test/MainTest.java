package de.onyx4j.test;

import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.OnyxClientBuilder;
import de.onyx4j.api.events.OnyxListenerAdapter;
import de.onyx4j.api.events.ReadyEvent;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.CommandOptionData;
import de.onyx4j.api.interactions.commands.Commands;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import de.onyx4j.test.showcase.ShowcaseListener;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * © Copyright 23.11.2026 - 22:29 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */

public final class MainTest
{

	private static final long STARTED_AT = System.nanoTime();

	static void main(String[] args) throws InterruptedException
	{
		String token = loadToken();

		if (token == null || token.isBlank())
			throw new IllegalStateException("ONYX_BOT_TOKEN is missing");

		OnyxClient client = OnyxClientBuilder.createDefault(token)
				.addCommands(Commands.slash("ping", "Test the Onyx4J gateway connection"),
						Commands.slash("about", "Display information about the Onyx4J SDK"),
						Commands.slash("status", "Display the current showcase bot status"),
						Commands.slash("whoami", "Display your Onyx account information"),
						Commands.slash("language", "Choose your programming language").addOptions(
								CommandOptionData.string("name", "Programming language").setRequired(true)
										.setAutocomplete(true)))
				.addEventListeners(new OnyxListenerAdapter()
		{
			@Override
			public void onReady(ReadyEvent event)
			{
				System.out.println("Onyx4J Showcase connected");

				if (event.isReconnect())
					System.out.println("Gateway reconnected");
			}

			@Override
			public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
			{
				switch (event.getName())
				{

				case "ping" -> event.reply("Pong! Onyx4J 0.4.0 | Java 25").queue();

				case "about" -> event.reply(
								"Onyx4J - Java API for Onyx\n" + "Version: 0.4.0-SNAPSHOT\n" + "Language: Java 25\n"
										+ "Developer: Sekuramis\n" + "Gateway: Socket.IO\n"
										+ "Features: Events, Slash Commands, REST, " + "Messages, Channels, Roles and Members")
						.queue();

				case "status" ->
				{
					long uptime = Duration.ofNanos(System.nanoTime() - STARTED_AT).toSeconds();

					event.reply(
							"Onyx4J Showcase Status\n" + "Runtime: Java " + Runtime.version().feature() + "\nUptime: "
									+ uptime + " seconds" + "\nSDK: 0.4.0-SNAPSHOT").queue();
				}

				case "whoami" ->
				{
					var user = event.getUser();

					if (user == null)
					{
						event.replyEphemeral("User information is unavailable.").queue();
						return;
					}

					event.replyEphemeral("Your Onyx Profile\n" + "Username: " + user.getName() + "\nDisplay Name: "
							+ user.getDisplayName() + "\nUser ID: " + user.getId()).queue();
				}

				default ->
				{
				}
				}
			}

			@Override
			public void onError(Throwable error)
			{
				error.printStackTrace();
			}
		}).addEventListeners(new ShowcaseListener()).build();

		Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown));

		client.awaitReady();
	}

	private static String loadToken()
	{
		Path path = Path.of("config.yml");

		if (!Files.isRegularFile(path))
		{
			throw new IllegalStateException("config.yml not found: " + path.toAbsolutePath());
		}

		try (InputStream input = Files.newInputStream(path))
		{
			Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));

			Object result = yaml.load(input);

			if (!(result instanceof Map<?, ?> config) || !(config.get("bot") instanceof Map<?, ?> bot) || !(bot.get(
					"token") instanceof String token) || token.isBlank())
			{
				throw new IllegalStateException("Invalid or missing bot.token in config.yml");
			}

			return token;
		} catch (IOException exception)
		{
			throw new IllegalStateException("Failed to read config.yml", exception);
		}
	}
}