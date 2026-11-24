package de.onyx4j.test;

import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.OnyxClientBuilder;
import de.onyx4j.api.concurrent.ServerTaskOptions;
import de.onyx4j.api.entities.User;
import de.onyx4j.api.entities.message.MessageBuilder;
import de.onyx4j.api.interactions.components.ButtonBuilder;
import de.onyx4j.api.interactions.components.ButtonStyle;
import de.onyx4j.api.interactions.components.ComponentRow;
import de.onyx4j.api.interactions.components.SelectMenuBuilder;
import de.onyx4j.api.entities.role.Permission;
import de.onyx4j.api.events.OnyxListenerAdapter;
import de.onyx4j.api.events.ReadyEvent;
import de.onyx4j.api.events.voice.VoiceJoinEvent;
import de.onyx4j.api.events.voice.VoiceLeaveEvent;
import de.onyx4j.api.events.EventPriority;
import de.onyx4j.api.events.DisconnectEvent;
import de.onyx4j.api.events.annotation.SubscribeEvent;
import de.onyx4j.api.events.interaction.AutocompleteInteractionEvent;
import de.onyx4j.api.events.interaction.ComponentInteractionEvent;
import de.onyx4j.api.events.interaction.ModalInteractionEvent;
import de.onyx4j.api.events.message.MessageReceivedEvent;
import de.onyx4j.api.interactions.annotation.*;
import de.onyx4j.api.interactions.commands.CommandOptionChoice;
import de.onyx4j.api.interactions.components.Components;
import de.onyx4j.api.interactions.modals.ModalForm;
import java.util.List;
import java.util.Locale;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.annotation.CommandOption;
import de.onyx4j.api.interactions.commands.annotation.SlashCommand;
import de.onyx4j.api.interactions.commands.annotation.RequirePermissions;
import de.onyx4j.api.interactions.commands.annotation.ServerOnly;
import de.onyx4j.api.interactions.commands.annotation.BotOwnerOnly;
import de.onyx4j.api.interactions.commands.annotation.Cooldown;
import de.onyx4j.api.interactions.commands.annotation.RequirePreconditions;
import de.onyx4j.api.interactions.commands.middleware.CommandPrecondition;
import de.onyx4j.api.interactions.commands.middleware.PreconditionResult;
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
		String ownerId = System.getenv("ONYX_BOT_OWNER_ID");

		OnyxClient client = OnyxClientBuilder
				.createDefault(loadToken())
				.addEventListeners(handler)
				.addCommandHandlers(handler)
				.addEventHandlers(handler)
				.setServerTaskOptions(new ServerTaskOptions(4, 256))
				.setBotOwnerIds(ownerId == null || ownerId.isBlank() ? new String[0] : new String[]{ownerId})
				.addInteractionHandlers(handler)
				.setAutoSyncCommands(true)
				.build();

		Runtime.getRuntime().addShutdownHook(
				new Thread(client::shutdown)
		);

		client.awaitReady();
	}

	@Override
	public void onReady(ReadyEvent event) {
		System.out.println("Onyx4J 0.17.0 connected!");
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
                Onyx4J 0.17.0-SNAPSHOT
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
                API Version: 0.17.0-SNAPSHOT
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

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.getMessage().isAuthorBot() || !"!interactions".equals(event.getMessage().getContentRaw())) return;
		event.getChannel().sendMessage(Components.message("Onyx4J 0.17.0 Interaction Showcase",
				Components.row(
						Components.primary("demo:ping", "Ping"),
						Components.secondary("demo:ticket:42", "Ticket 42"),
						Components.secondary("demo:feedback", "Feedback")),
				Components.row(Components.select("demo:language", "Choose a language",
						Components.option("Java", "java"),
						Components.option("Kotlin", "kotlin"))),
				Components.row(Components.primary("demo:generic", "Generic component"))
		)).queue(message -> {}, this::onError);
	}

	@ButtonInteraction("demo:ping")
	public void buttonPing(ComponentInteractionEvent event) {
		event.replyEphemeral("Pong from a button!").queue(result -> {}, this::onError);
	}

	@ButtonInteraction("demo:ticket:{ticketId}")
	public void ticket(ComponentInteractionEvent event, @PathParam("ticketId") long ticketId) {
		event.replyEphemeral("Ticket ID: " + ticketId).queue(result -> {}, this::onError);
	}

	@SelectInteraction("demo:language")
	public void language(ComponentInteractionEvent event) {
		event.replyEphemeral("Selected: " + String.join(", ", event.getValues())).queue(result -> {}, this::onError);
	}

	@ButtonInteraction("demo:feedback")
	public void feedbackButton(ComponentInteractionEvent event) {
		event.openModal(ModalForm.create("demo:feedback:submit", "Feedback")
				.paragraphField("feedback", "Your feedback", true)
				.submitLabel("Send")).queue(result -> {}, this::onError);
	}

	@ModalInteraction("demo:feedback:submit")
	public void feedback(ModalInteractionEvent event, @ModalField("feedback") String feedback) {
		event.replyEphemeral("Feedback received (" + feedback.length() + " characters).")
				.queue(result -> {}, this::onError);
	}

	@ComponentInteraction("demo:generic")
	public void generic(ComponentInteractionEvent event) {
		event.replyEphemeral("Component type: " + event.getComponentType()).queue(result -> {}, this::onError);
	}

	@SlashCommand(name = "search", description = "Test autocomplete")
	public void search(SlashCommandInteractionEvent event,
			@CommandOption(name = "query", description = "Search languages", autocomplete = true) String query) {
		event.reply("Search: " + query).queue(result -> {}, this::onError);
	}

	@Autocomplete(command = "search", option = "query")
	public void autocomplete(AutocompleteInteractionEvent event) {
		String query = event.getFocusedValue().toLowerCase(Locale.ROOT);
		List<CommandOptionChoice> choices = List.of("Java", "Kotlin", "Rust", "Python", "JavaScript")
				.stream().filter(value -> value.toLowerCase(Locale.ROOT).contains(query))
				.map(value -> CommandOptionChoice.of(value, value)).toList();
		event.replyChoices(choices).queue(result -> {}, this::onError);
	}

	@SlashCommand(name = "permissioncheck", description = "Test permission-based command access")
	@RequirePermissions(Permission.MANAGE_ROLES)
	public void permissionCheck(SlashCommandInteractionEvent event) {
		event.replyEphemeral("Permission MANAGE_ROLES granted.").queue(result -> {}, this::onError);
	}

	@SlashCommand(name = "servercheck", description = "Test server-only command access")
	@ServerOnly
	public void serverCheck(SlashCommandInteractionEvent event) {
		event.replyEphemeral("Server ID: " + event.getServerId()).queue(result -> {}, this::onError);
	}

	@SlashCommand(name = "cooldowncheck", description = "Test command cooldown")
	@Cooldown(seconds = 10)
	public void cooldownCheck(SlashCommandInteractionEvent event) {
		event.replyEphemeral("Cooldown started: 10 seconds.").queue(result -> {}, this::onError);
	}

	@SlashCommand(name = "ownercheck", description = "Test bot owner access")
	@BotOwnerOnly
	public void ownerCheck(SlashCommandInteractionEvent event) {
		event.replyEphemeral("Bot owner access granted.").queue(result -> {}, this::onError);
	}

	@SlashCommand(name = "preconditioncheck", description = "Test custom command preconditions")
	@RequirePreconditions(ServerContext.class)
	public void preconditionCheck(SlashCommandInteractionEvent event) {
		event.replyEphemeral("Precondition passed.").queue(result -> {}, this::onError);
	}

	@SlashCommand(name = "errorcheck", description = "Test central exception handling")
	public void errorCheck(SlashCommandInteractionEvent event) {
		throw new IllegalStateException("Intentional middleware showcase error");
	}

	public static final class ServerContext implements CommandPrecondition {
		@Override
		public PreconditionResult check(SlashCommandInteractionEvent event) {
			return event.getServerId() == null || event.getServerId().isBlank()
					? PreconditionResult.deny("A server context is required.")
					: PreconditionResult.allow();
		}
	}

	@SlashCommand(name = "messagecheck", description = "Test the advanced message builder")
	public void messageCheck(SlashCommandInteractionEvent event) {
		event.reply(new MessageBuilder()
				.appendLine("**Onyx4J 0.17.0**")
				.append("MessageBuilder showcase")
				.addRow(ComponentRow.buttons(
						ButtonBuilder.of(ButtonStyle.PRIMARY, "demo:ping", "Ping"),
						ButtonBuilder.of(ButtonStyle.SECONDARY, "demo:feedback", "Feedback")))
				.addRow(ComponentRow.select(new SelectMenuBuilder("demo:language")
						.setPlaceholder("Choose a language")
						.addOption("Java", "java")
						.addOption("Kotlin", "kotlin")))
				.build()).queue(result -> {}, this::onError);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void eventReady(ReadyEvent event) {
		System.out.println("Annotated ready event (reconnect=" + event.isReconnect() + ")");
	}

	@SubscribeEvent(priority = EventPriority.NORMAL, async = true)
	public void eventCheck(MessageReceivedEvent event) {
		if (event.getMessage().isAuthorBot() || !"!eventcheck".equals(event.getMessage().getContentRaw())) return;
		event.getMessage().reply("Annotated event received on " + Thread.currentThread().getName())
				.queue(result -> {}, this::onError);
	}

	@SubscribeEvent
	public void eventDisconnected(DisconnectEvent event) {
		System.out.println("Annotated disconnect: " + event.getReason());
	}

	@SlashCommand(name = "health", description = "Show gateway and REST health")
	public void health(SlashCommandInteractionEvent event) {
		var gateway = event.getClient().getGatewayStatus();
		var rest = event.getClient().getRestStatistics();
		event.replyEphemeral("""
				Gateway: %s
				Connections: %d | Reconnects: %d
				Reconnect attempts: %d | Errors: %d
				Heartbeats: %d | Timeouts: %d
				Last heartbeat: %s
				REST requests: %d | Attempts: %d
				Success: %d | Failed: %d
				Retries: %d | HTTP 429: %d
				In flight: %d | Average: %d ms
				""".formatted(gateway.getState(), gateway.getConnectionCount(), gateway.getReconnectCount(),
				gateway.getReconnectAttempts(), gateway.getConnectionErrors(),
				gateway.getHeartbeatCount(), gateway.getHeartbeatTimeoutCount(), gateway.getLastHeartbeatAt(),
				rest.requests(), rest.attempts(), rest.successful(), rest.failed(),
				rest.retries(), rest.rateLimited(), rest.inFlight(), rest.averageDurationMillis()))
				.queue(result -> {}, this::onError);
	}

	@SubscribeEvent
	public void voiceJoin(VoiceJoinEvent event) {
		System.out.println("Voice join: " + event.getUserId() + " -> " + event.getChannelId());
	}

	@SubscribeEvent
	public void voiceLeave(VoiceLeaveEvent event) {
		System.out.println("Voice leave: " + event.getUserId() + " -> " + event.getChannelId()
				+ " (" + event.getDurationMillis() + " ms)");
	}

	@SlashCommand(name = "voicecheck", description = "Show observed voice states")
	@ServerOnly
	public void voiceCheck(SlashCommandInteractionEvent event) {
		var states = event.getClient().getVoiceStateCache().getServerStates(event.getServerId());
		event.replyEphemeral("Observed voice participants since connection: " + states.size())
				.queue(result -> {}, this::onError);
	}

	@SlashCommand(name = "scalecheck", description = "Test server-partitioned task execution")
	@ServerOnly
	public void scaleCheck(SlashCommandInteractionEvent event) {
		var executor = event.getClient().getServerTaskExecutor();
		executor.submit(event.getServerId(), () -> {
			var stats = executor.getStatistics();
			return """
					Server partition: %d / %d
					Worker: %s
					Queued tasks: %d
					Running tasks: %d
					Rejected tasks: %d
					""".formatted(executor.getPartition(event.getServerId()), stats.partitions(),
					Thread.currentThread().getName(), stats.queued(), stats.running(), stats.rejected());
		}).whenComplete((result, error) -> {
			if (error != null) {
				onError(error);
				event.replyEphemeral("Server task failed.").queue(response -> {}, this::onError);
			} else {
				event.replyEphemeral(result).queue(response -> {}, this::onError);
			}
		});
	}

	@SlashCommand(name = "installedservers", description = "Show installed servers")
	@BotOwnerOnly
	public void installedServers(SlashCommandInteractionEvent event) {
		event.getClient().retrieveInstalledServers().queue(servers ->
				event.replyEphemeral("Installed servers: " + servers.size())
						.queue(response -> {}, this::onError), error -> {
					onError(error);
					event.replyEphemeral("Unable to retrieve installed servers.").queue(response -> {}, this::onError);
				});
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
