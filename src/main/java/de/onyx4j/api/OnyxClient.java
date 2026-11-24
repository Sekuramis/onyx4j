package de.onyx4j.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.onyx4j.api.entities.channel.ServerChannel;
import de.onyx4j.api.entities.channel.TextChannel;
import de.onyx4j.api.entities.channel.VoiceChannel;
import de.onyx4j.api.entities.server.Server;
import de.onyx4j.api.entities.member.Member;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.events.GatewayEvent;
import de.onyx4j.api.events.EventBus;
import de.onyx4j.api.events.DisconnectEvent;
import de.onyx4j.api.events.OnyxListenerAdapter;
import de.onyx4j.api.events.ReadyEvent;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.events.member.MemberJoinEvent;
import de.onyx4j.api.events.message.MessageReceivedEvent;
import de.onyx4j.api.gateway.OnyxGateway;
import de.onyx4j.api.requests.CommandUpdateAction;
import de.onyx4j.api.requests.OnyxRestClient;
import de.onyx4j.api.requests.RestOptions;
import de.onyx4j.api.requests.RestStatistics;
import de.onyx4j.api.requests.RestAction;
import de.onyx4j.api.events.interaction.ComponentInteractionEvent;
import de.onyx4j.api.events.interaction.ModalInteractionEvent;
import de.onyx4j.api.events.interaction.AutocompleteInteractionEvent;
import de.onyx4j.api.interactions.commands.CommandRegistry;
import de.onyx4j.api.events.interaction.InteractionAutoDefer;
import de.onyx4j.api.cache.EntityCache;
import de.onyx4j.api.cache.VoiceStateCache;
import de.onyx4j.api.events.voice.VoiceJoinEvent;
import de.onyx4j.api.events.voice.VoiceLeaveEvent;
import de.onyx4j.api.entities.voice.VoiceToken;
import de.onyx4j.api.gateway.EventFilter;
import de.onyx4j.api.gateway.GatewayStatus;
import de.onyx4j.api.events.gateway.GatewayEventBus;
import de.onyx4j.api.events.gateway.EventSubscription;
import de.onyx4j.api.interactions.commands.CommandRouter;
import de.onyx4j.api.interactions.InteractionRouter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.time.Duration;
import java.util.function.Predicate;

/**
 * © Copyright 23.11.2026 - 16:51 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class OnyxClient
{

	private final OnyxRestClient rest;
	private final OnyxGateway gateway;
	private final List<OnyxListenerAdapter> listeners;
	private final CompletableFuture<OnyxClient> ready;
	private final AtomicBoolean started;
	private final CommandRegistry commandRegistry;
	private final CommandRouter commandRouter;
	private final InteractionRouter interactionRouter = new InteractionRouter();
	private final InteractionAutoDefer autoDefer = new InteractionAutoDefer(2500);
	private final EntityCache entityCache;
	private final VoiceStateCache voiceStateCache = new VoiceStateCache();
	private final GatewayStatus gatewayStatus = new GatewayStatus();
	private volatile EventFilter eventFilter = EventFilter.all();
	private final GatewayEventBus gatewayEventBus;
	private final EventBus eventBus;

	private final Object lifecycleLock = new Object();
	private volatile boolean connected;
	private volatile boolean everReady;
	private volatile boolean shuttingDown;

	OnyxClient(String token, String baseUrl, List<OnyxListenerAdapter> listeners)
	{
		this(token, baseUrl, listeners, RestOptions.defaults());
	}

	OnyxClient(String token, String baseUrl, List<OnyxListenerAdapter> listeners, RestOptions restOptions)
	{
		this.rest = new OnyxRestClient(token, baseUrl, restOptions);
		this.commandRegistry = new CommandRegistry(rest);
		this.commandRouter = new CommandRouter(commandRegistry, this::handleError);
		this.commandRouter.setExceptionHandler((event, error) -> event.replyEphemeral("Command execution failed.")
				.queue(result -> {}, this::handleError));
		this.listeners = new CopyOnWriteArrayList<>(listeners);
		this.ready = new CompletableFuture<>();
		this.started = new AtomicBoolean();
		this.entityCache = new EntityCache(this);

		this.gateway = new OnyxGateway(baseUrl, token, this::handleReady, this::handleGatewayEvent,
				this::handleInteraction, this::handleDisconnect, this::handleError, gatewayStatus);
		this.gatewayEventBus = new GatewayEventBus(this::handleError);
		this.eventBus = new EventBus(this::handleError);
	}

	public void connect()
	{
		synchronized (lifecycleLock) {
			if (shuttingDown) throw new IllegalStateException("Client was shut down");
			if (!started.compareAndSet(false, true)) return;
			gatewayStatus.markConnecting();
		}

		ready.orTimeout(60, java.util.concurrent.TimeUnit.SECONDS).whenComplete((client, failure) -> {
			if (failure instanceof java.util.concurrent.TimeoutException) {
				shutdown();
				handleError(failure);
			}
		});
		rest.get("/apps/@me").thenCompose(result -> {
			synchronized (lifecycleLock) {
				if (shuttingDown) return CompletableFuture.failedFuture(new IllegalStateException("Client was shut down"));
				return commandRegistry.syncIfEnabled();
			}
		}).whenComplete((result, error) -> {
			Throwable failure = error;
			synchronized (lifecycleLock) {
				if (shuttingDown) return;
				if (failure == null) {
					try {
						gateway.connect();
						return;
					} catch (Exception exception) {
						failure = exception;
					}
				}
				gatewayStatus.markDisconnected("Connection initialization failed");
			}
			ready.completeExceptionally(failure);
			handleError(failure);
		});
	}

	private void handleReady(JsonObject data)
	{
		boolean reconnect;
		synchronized (lifecycleLock) {
			if (shuttingDown || connected) return;
			reconnect = everReady;
			everReady = true;
			connected = true;
			if (reconnect) {
				entityCache.clear();
				voiceStateCache.clear();
			}
			gatewayStatus.markReady();
		}

		ReadyEvent event = new ReadyEvent(this, reconnect, data);

		ready.complete(this);
		eventBus.publish(event);
		dispatch(listener -> listener.onReady(event));

		if (reconnect)
			dispatch(listener -> listener.onReconnect(event));
	}

	private void handleGatewayEvent(JsonObject payload)
	{
		if (shuttingDown || !payload.has("type"))
			return;

		GatewayEvent event = new GatewayEvent(this, payload);
		String type = event.getType();

		GatewayEvent voiceEvent = event;
		try {
			if ("voice.joined".equals(type)) voiceEvent = new VoiceJoinEvent(this, payload);
			else if ("voice.left".equals(type)) voiceEvent = new VoiceLeaveEvent(this, payload);
		} catch (RuntimeException exception) {
			handleError(exception);
		}

		synchronized (lifecycleLock) {
			if (shuttingDown) return;
			entityCache.apply(event);
			voiceStateCache.apply(voiceEvent);
		}

		if (!eventFilter.allows(event))
			return;

		gatewayEventBus.publish(event);

		dispatch(listener -> listener.onGatewayEvent(event));

		if (type == null)
			return;

		switch (type)
		{
		case "message.created" ->
		{
			MessageReceivedEvent messageEvent = new MessageReceivedEvent(this, payload);

			eventBus.publish(messageEvent);
			dispatch(listener -> listener.onMessageReceived(messageEvent));
		}

		case "member.joined" ->
		{
			MemberJoinEvent memberEvent = new MemberJoinEvent(this, payload);

			eventBus.publish(memberEvent);
			dispatch(listener -> listener.onMemberJoin(memberEvent));
		}

		case "voice.joined" -> {
			if (voiceEvent instanceof VoiceJoinEvent join) {
				eventBus.publish(join);
				dispatch(listener -> listener.onVoiceJoin(join));
			} else eventBus.publish(event);
		}
		case "voice.left" -> {
			if (voiceEvent instanceof VoiceLeaveEvent leave) {
				eventBus.publish(leave);
				dispatch(listener -> listener.onVoiceLeave(leave));
			} else eventBus.publish(event);
		}
		default -> eventBus.publish(event);
		}
	}

	private void handleInteraction(JsonObject payload)
	{
		if (shuttingDown)
			return;

		if (!payload.has("type") || payload.get("type").isJsonNull())
			return;

		String type = payload.get("type").getAsString();

		switch (type)
		{
		case "component" ->
		{
			ComponentInteractionEvent event = new ComponentInteractionEvent(this, payload);

			autoDefer.schedule(event::isAcknowledged, () -> event.deferReply().queue(), this::handleError);

			try
			{
				if (!interactionRouter.dispatch(event)) {
					eventBus.publish(event);
					dispatch(listener -> listener.onComponentInteraction(event));
				}
			} catch (Exception exception)
			{
				handleError(exception);
			}
		}

		case "modal" ->
		{
			ModalInteractionEvent event = new ModalInteractionEvent(this, payload);

			autoDefer.schedule(event::isAcknowledged, () -> event.deferReply().queue(), this::handleError);

			try
			{
				if (!interactionRouter.dispatch(event)) {
					eventBus.publish(event);
					dispatch(listener -> listener.onModalInteraction(event));
				}
			} catch (Exception exception)
			{
				handleError(exception);
			}
		}

		case "command" ->
		{
			if (!payload.has("command") || !payload.get("command").isJsonObject())
				return;

			JsonObject command = payload.getAsJsonObject("command");

			if (command.has("type") && !command.get("type").getAsString().equals("slash"))
				return;

			SlashCommandInteractionEvent event = new SlashCommandInteractionEvent(this, payload);

			autoDefer.schedule(
					event::isAcknowledged,
					() -> event.deferReply().queue(),
					this::handleError
			);

			try
			{
				if (!commandRouter.dispatch(event)) {
					eventBus.publish(event);
					dispatch(listener -> listener.onSlashCommandInteraction(event));
				}
			} catch (Exception exception)
			{
				handleError(exception);
			}
		}

		case "autocomplete" ->
		{
			AutocompleteInteractionEvent event = new AutocompleteInteractionEvent(this, payload);

			autoDefer.schedule(event::isAcknowledged, () -> event.replyChoices().queue(), this::handleError);

			try
			{
				if (!interactionRouter.dispatch(event)) {
					eventBus.publish(event);
					dispatch(listener -> listener.onAutocompleteInteraction(event));
				}
			} catch (Exception exception)
			{
				handleError(exception);
			}
		}

		default ->
		{
		}
		}
	}

	private void handleDisconnect(String reason)
	{
		synchronized (lifecycleLock) {
			if (shuttingDown) return;
			connected = false;
			gatewayStatus.markDisconnected(reason);
			entityCache.clear();
			voiceStateCache.clear();
		}
		eventBus.publish(new DisconnectEvent(this, reason));
		dispatch(listener -> listener.onDisconnect(reason));

	}

	private void handleError(Throwable error)
	{
		for (OnyxListenerAdapter listener : listeners)
		{
			try
			{
				listener.onError(error);
			} catch (Exception exception)
			{
				exception.printStackTrace();
			}
		}
	}

	private void dispatch(Consumer<OnyxListenerAdapter> action)
	{
		for (OnyxListenerAdapter listener : listeners)
		{
			try
			{
				action.accept(listener);
			} catch (Exception exception)
			{
				handleError(exception);
			}
		}
	}

	public OnyxClient awaitReady()
	{
		return ready.join();
	}

	public RestStatistics getRestStatistics()
	{
		return rest.getStatistics();
	}

	public CompletableFuture<OnyxClient> getReadyFuture()
	{
		return ready;
	}

	public boolean isConnected()
	{
		return connected;
	}

	public void addEventListener(OnyxListenerAdapter listener)
	{
		listeners.add(listener);
	}

	public TextChannel getTextChannelById(String channelId)
	{
		return new TextChannel(this, channelId);
	}

	public VoiceStateCache getVoiceStateCache()
	{
		return voiceStateCache;
	}

	public RestAction<VoiceToken> requestVoiceToken(String channelId)
	{
		String path = "/channels/" + EntityJson.encode(channelId) + "/voice-token";
		return new RestAction<>(() -> rest.post(path, new JsonObject())
				.thenApply(response -> new VoiceToken(EntityJson.requireObject(response))));
	}

	public VoiceChannel getVoiceChannelById(String channelId)
	{
		return new VoiceChannel(this, channelId);
	}

	public RestAction<Member> retrieveMemberById(String serverId, String userId)
	{
		String path = "/servers/" + EntityJson.encode(serverId) + "/members/" + EntityJson.encode(userId);
		return new RestAction<>(() -> rest.get(path)
				.thenApply(response -> new Member(this, serverId, EntityJson.requireObject(response))));
	}

	public RestAction<Server> retrieveServerById(String serverId)
	{
		EntityJson.encode(serverId);
		return new RestAction<>(() -> rest.get("/servers/" + EntityJson.encode(serverId))
				.thenApply(response -> new Server(this, EntityJson.requireObject(response))));
	}

	public RestAction<ServerChannel> retrieveChannelById(String channelId)
	{
		EntityJson.encode(channelId);
		return new RestAction<>(() -> rest.get("/channels/" + EntityJson.encode(channelId)).thenApply(response ->
		{
			JsonObject data = EntityJson.requireObject(response);
			String type = EntityJson.string(data, "type");
			if ("text".equals(type))
				return new TextChannel(this, data);
			if ("voice".equals(type))
				return new VoiceChannel(this, data);
			throw new IllegalStateException("Unknown Onyx channel type: " + type);
		}));
	}

	public RestAction<JsonElement> sendMessage(String channelId, String content)
	{
		return new RestAction<>(() ->
		{
			JsonObject payload = new JsonObject();
			payload.addProperty("content", content);

			return rest.post("/channels/" + channelId + "/messages", payload);
		});
	}

	public CommandUpdateAction updateCommands()
	{
		return new CommandUpdateAction(this);
	}

	public OnyxRestClient getRestClient()
	{
		return rest;
	}

	public void shutdown()
	{
		synchronized (lifecycleLock) {
			if (shuttingDown) return;
			shuttingDown = true;
			connected = false;
			gatewayStatus.markShutdown();
			entityCache.clear();
			voiceStateCache.clear();
		}

		if (!ready.isDone())
		{
			ready.completeExceptionally(new IllegalStateException("Client was shut down"));
		}

		gateway.disconnect();
		rest.close();
		autoDefer.close();
		gatewayEventBus.close();
		eventBus.close();
	}

	public CommandRegistry getCommandRegistry()
	{
		return commandRegistry;
	}

	public EntityCache getEntityCache()
	{
		return entityCache;
	}

	public GatewayStatus getGatewayStatus()
	{
		return gatewayStatus;
	}

	public void setEventFilter(EventFilter filter)
	{
		this.eventFilter = java.util.Objects.requireNonNull(filter);
	}

	public EventBus getEventBus()
	{
		return eventBus;
	}

	public EventSubscription addEventHandler(Object handler)
	{
		return eventBus.register(handler);
	}

	public boolean removeEventHandler(Object handler)
	{
		return eventBus.unregister(handler);
	}

	public GatewayEventBus getGatewayEventBus()
	{
		return gatewayEventBus;
	}

	public EventSubscription onGatewayEvent(String type, Consumer<GatewayEvent> handler)
	{
		return gatewayEventBus.on(type, handler);
	}

	public EventSubscription onceGatewayEvent(String type, Consumer<GatewayEvent> handler)
	{
		return gatewayEventBus.once(type, handler);
	}

	public CompletableFuture<GatewayEvent> waitForGatewayEvent(String type, Predicate<GatewayEvent> filter,
			Duration timeout)
	{
		return gatewayEventBus.waitFor(type, filter, timeout);
	}

	public InteractionRouter getInteractionRouter()
	{
		return interactionRouter;
	}

	public CommandRouter getCommandRouter()
	{
		return commandRouter;
	}

}
