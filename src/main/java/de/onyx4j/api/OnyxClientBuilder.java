package de.onyx4j.api;

import de.onyx4j.api.events.GatewayEvent;
import de.onyx4j.api.events.OnyxListenerAdapter;
import de.onyx4j.api.events.gateway.GatewayEventBus;
import de.onyx4j.api.interactions.commands.SlashCommandData;
import de.onyx4j.api.requests.RestOptions;
import de.onyx4j.api.entities.presence.Presence;
import de.onyx4j.api.concurrent.ServerTaskOptions;
import de.onyx4j.api.gateway.EventFilter;
import de.onyx4j.api.interactions.commands.middleware.CommandExceptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * © Copyright 23.11.2026 - 16:51 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class OnyxClientBuilder
{

	private final String token;
	private String baseUrl = "https://onyx-clips.de";

	private final java.util.List<SlashCommandData> registeredCommands = new java.util.ArrayList<>();
	private boolean autoSyncCommands = true;
	private RestOptions restOptions = RestOptions.defaults();
	private ServerTaskOptions serverTaskOptions = ServerTaskOptions.defaults();
	private EventFilter eventFilter = EventFilter.all();
	private Presence presence;

	private final List<OnyxListenerAdapter> listeners = new ArrayList<>();
	private final List<Object> commandHandlers = new ArrayList<>();
	private final List<Object> interactionHandlers = new ArrayList<>();
	private final List<Object> eventHandlers = new ArrayList<>();
	private List<String> botOwnerIds = List.of();
	private CommandExceptionHandler commandExceptionHandler;

	private OnyxClientBuilder(String token)
	{
		if (token == null || token.isBlank())
		{
			throw new IllegalArgumentException("An Onyx app token is required");
		}

		this.token = token;
	}

	public static OnyxClientBuilder createDefault(String token)
	{
		return new OnyxClientBuilder(token);
	}

	public OnyxClientBuilder setBaseUrl(String baseUrl)
	{
		this.baseUrl = Objects.requireNonNull(baseUrl);
		return this;
	}

	public OnyxClientBuilder addEventListeners(OnyxListenerAdapter... listeners)
	{
		this.listeners.addAll(Arrays.asList(listeners));
		return this;
	}

	public OnyxClient build() {
		OnyxClient client = new OnyxClient(
				token,
				baseUrl,
				List.copyOf(listeners),
				restOptions,
				serverTaskOptions
		);
		try {
		client.setEventFilter(eventFilter);
		client.configurePresence(presence);

		client.getCommandRegistry()
				.addCommands(registeredCommands.toArray(SlashCommandData[]::new))
				.setAutoSync(autoSyncCommands);

		client.getCommandRouter().setBotOwnerIds(botOwnerIds.toArray(String[]::new));
		if (commandExceptionHandler != null) client.getCommandRouter().setExceptionHandler(commandExceptionHandler);
		client.getCommandRouter().registerHandlers(commandHandlers.toArray());
		client.getInteractionRouter().registerHandlers(interactionHandlers.toArray());
		for (Object handler : eventHandlers) client.addEventHandler(handler);
		gatewayListeners.forEach(registration -> registration.accept(client.getGatewayEventBus()));
		client.connect();
		return client;
		} catch (RuntimeException | Error error) {
			client.shutdown();
			throw error;
		}
	}

	public OnyxClientBuilder addCommands(SlashCommandData... commands)
	{
		registeredCommands.addAll(java.util.List.of(commands));
		return this;
	}

	public OnyxClientBuilder setServerTaskOptions(ServerTaskOptions options)
	{
		serverTaskOptions = Objects.requireNonNull(options, "options");
		return this;
	}

	public OnyxClientBuilder setEventFilter(EventFilter filter)
	{
		eventFilter = Objects.requireNonNull(filter, "filter");
		return this;
	}

	public OnyxClientBuilder setPresence(Presence presence)
	{
		this.presence = Objects.requireNonNull(presence, "presence");
		return this;
	}

	public OnyxClientBuilder setRestOptions(RestOptions options)
	{
		this.restOptions = Objects.requireNonNull(options, "options");
		return this;
	}

	public OnyxClientBuilder setAutoSyncCommands(boolean enabled)
	{
		autoSyncCommands = enabled;
		return this;
	}

	private final List<Consumer<GatewayEventBus>> gatewayListeners = new ArrayList<>();

	public OnyxClientBuilder onGatewayEvent(String type, Consumer<GatewayEvent> handler)
	{
		if (type == null || type.isBlank())
		{
			throw new IllegalArgumentException("Event type must not be blank");
		}

		Objects.requireNonNull(handler, "handler");

		gatewayListeners.add(bus -> bus.on(type, handler));
		return this;
	}

	public OnyxClientBuilder addEventHandlers(Object... handlers) {
		eventHandlers.addAll(List.of(handlers));
		return this;
	}

	public OnyxClientBuilder addInteractionHandlers(Object... handlers) {
		interactionHandlers.addAll(List.of(handlers));
		return this;
	}

	public OnyxClientBuilder setBotOwnerIds(String... ids) {
		botOwnerIds = List.of(ids);
		if (botOwnerIds.stream().anyMatch(String::isBlank))
			throw new IllegalArgumentException("Bot owner ID must not be blank");
		return this;
	}

	public OnyxClientBuilder setCommandExceptionHandler(CommandExceptionHandler handler) {
		commandExceptionHandler = Objects.requireNonNull(handler, "handler");
		return this;
	}

	public OnyxClientBuilder addCommandHandlers(Object... handlers) {
		commandHandlers.addAll(List.of(handlers));
		return this;
	}

}
