package de.onyx4j.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.onyx4j.api.entities.channel.ServerChannel;
import de.onyx4j.api.entities.channel.TextChannel;
import de.onyx4j.api.entities.channel.VoiceChannel;
import de.onyx4j.api.entities.server.Server;
import de.onyx4j.api.entities.message.Attachment;
import de.onyx4j.api.entities.Message;
import de.onyx4j.api.entities.message.MessageData;
import de.onyx4j.api.entities.message.MessageBuilder;
import de.onyx4j.api.entities.message.DirectMessage;
import de.onyx4j.api.entities.message.ScheduledMessage;
import de.onyx4j.api.entities.message.ScheduleRepeat;
import de.onyx4j.api.entities.panel.Panel;
import de.onyx4j.api.entities.panel.PanelPlacement;
import de.onyx4j.api.entities.presence.Presence;
import java.time.Instant;
import java.util.Objects;
import java.nio.file.Path;
import de.onyx4j.api.entities.server.InstalledServer;
import de.onyx4j.api.concurrent.ServerTaskExecutor;
import de.onyx4j.api.concurrent.ServerTaskOptions;
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
	private final ServerTaskExecutor serverTaskExecutor;

	private final Object lifecycleLock = new Object();
	private final Object presenceLock = new Object();
	private Presence desiredPresence;
	private CompletableFuture<Void> presenceTail = CompletableFuture.completedFuture(null);
	private volatile boolean connected;
	private volatile boolean everReady;
	private volatile boolean shuttingDown;

	OnyxClient(String token, String baseUrl, List<OnyxListenerAdapter> listeners)
	{
		this(token, baseUrl, listeners, RestOptions.defaults());
	}

	OnyxClient(String token, String baseUrl, List<OnyxListenerAdapter> listeners, RestOptions restOptions)
	{
		this(token, baseUrl, listeners, restOptions, ServerTaskOptions.defaults());
	}

	OnyxClient(String token, String baseUrl, List<OnyxListenerAdapter> listeners,
			RestOptions restOptions, ServerTaskOptions taskOptions)
	{
		this.rest = new OnyxRestClient(token, baseUrl, restOptions);
		this.serverTaskExecutor = new ServerTaskExecutor(taskOptions);
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

		applyConfiguredPresence();
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

	void configurePresence(Presence presence)
	{
		synchronized (presenceLock) {
			desiredPresence = presence;
		}
	}

	private void applyConfiguredPresence()
	{
		CompletableFuture<Void> future;
		synchronized (presenceLock) {
			if (desiredPresence == null || shuttingDown) return;
			future = enqueuePresence(desiredPresence);
		}
		future.whenComplete((result, error) -> {
			if (error != null && !shuttingDown) handleError(error);
		});
	}

	private CompletableFuture<Void> enqueuePresence(Presence presence)
	{
		JsonObject body = presence.toJson();
		presenceTail = presenceTail.handle((result, error) -> null)
				.thenCompose(ignored -> rest.patch("/apps/@me/presence", body))
				.thenApply(result -> null);
		return presenceTail;
	}

	public RestAction<Void> setPresence(Presence presence)
	{
		Objects.requireNonNull(presence, "presence");
		return new RestAction<>(() -> {
			synchronized (presenceLock) {
				if (shuttingDown) return CompletableFuture.failedFuture(new IllegalStateException("Client was shut down"));
				desiredPresence = desiredPresence == null ? presence : desiredPresence.merge(presence);
				return enqueuePresence(presence);
			}
		});
	}

	public RestAction<Void> clearActivity()
	{
		return setPresence(Presence.activity(null));
	}

	public RestAction<DirectMessage> sendDirectMessage(String userId, String content)
	{
		return sendDirectMessage(userId, new MessageBuilder().setContent(content).build());
	}

	public RestAction<DirectMessage> sendDirectMessage(String userId, MessageData message)
	{
		String path = "/users/" + EntityJson.encode(userId) + "/dm";
		JsonObject body = Objects.requireNonNull(message, "message").toJson();
		return new RestAction<>(() -> rest.post(path, body).thenApply(response -> {
			JsonObject data = EntityJson.requireObject(response);
			String channelId = EntityJson.string(data, "channel_id");
			JsonObject sent = EntityJson.requireObject(data.get("message")).deepCopy();
			if (!sent.has("channel_id") || sent.get("channel_id").isJsonNull())
				sent.addProperty("channel_id", channelId);
			return new DirectMessage(channelId, new Message(this, sent));
		}));
	}

	public RestAction<List<Panel>> retrievePanels(String channelId)
	{
		String path = "/channels/" + EntityJson.encode(channelId) + "/panels";
		return new RestAction<>(() -> rest.get(path).thenApply(response -> {
			var panels = new java.util.ArrayList<Panel>();
			for (JsonElement item : EntityJson.requireArray(response))
				panels.add(new Panel(this, EntityJson.requireObject(item)));
			return List.copyOf(panels);
		}));
	}

	public RestAction<Panel> createPanel(String channelId, String title, String url,
			PanelPlacement placement, JsonObject state)
	{
		String path = "/channels/" + EntityJson.encode(channelId) + "/panels";
		if (url == null || url.isBlank()) throw new IllegalArgumentException("Panel URL is required");
		JsonObject body = new JsonObject();
		if (title != null) body.addProperty("title", title);
		body.addProperty("url", url);
		if (placement != null) body.addProperty("placement", placement.getValue());
		if (state != null) body.add("state", state.deepCopy());
		return new RestAction<>(() -> rest.post(path, body)
				.thenApply(response -> new Panel(this, EntityJson.requireObject(response))));
	}

	public RestAction<List<ScheduledMessage>> retrieveScheduledMessages(String channelId)
	{
		String path = "/channels/" + EntityJson.encode(channelId) + "/scheduled";
		return new RestAction<>(() -> rest.get(path).thenApply(response -> {
			var messages = new java.util.ArrayList<ScheduledMessage>();
			for (JsonElement item : EntityJson.requireArray(response))
				messages.add(new ScheduledMessage(this, EntityJson.requireObject(item)));
			return List.copyOf(messages);
		}));
	}

	public RestAction<ScheduledMessage> scheduleMessage(String channelId, MessageData message,
			Instant sendAt, ScheduleRepeat repeat)
	{
		String path = "/channels/" + EntityJson.encode(channelId) + "/scheduled";
		JsonObject body = Objects.requireNonNull(message, "message").toJson();
		Objects.requireNonNull(sendAt, "sendAt");
		Objects.requireNonNull(repeat, "repeat");
		if (body.keySet().stream().anyMatch(key -> !key.equals("content") && !key.equals("embeds")))
			throw new IllegalArgumentException("Scheduled messages support content and embeds only");
		body.addProperty("send_at", sendAt.toEpochMilli());
		body.addProperty("repeat", repeat.getValue());
		return new RestAction<>(() -> {
			if (sendAt.isBefore(Instant.now().plusSeconds(60)))
				return CompletableFuture.failedFuture(new IllegalArgumentException("Scheduled time must be at least one minute in the future"));
			return rest.post(path, body).thenApply(response ->
					new ScheduledMessage(this, EntityJson.requireObject(response)));
		});
	}

	public RestAction<Attachment> uploadFile(Path file)
	{
		return uploadFile(file, null, null);
	}

	public RestAction<Attachment> uploadFile(Path file, String name, String type)
	{
		java.util.Objects.requireNonNull(file, "file");
		return new RestAction<>(() -> rest.upload(file, name, type)
				.thenApply(response -> Attachment.fromJson(EntityJson.requireObject(response))));
	}

	public RestAction<Attachment> uploadFile(byte[] bytes, String name, String type)
	{
		byte[] snapshot = java.util.Objects.requireNonNull(bytes, "bytes").clone();
		return new RestAction<>(() -> rest.upload(snapshot, name, type)
				.thenApply(response -> Attachment.fromJson(EntityJson.requireObject(response))));
	}

	public ServerTaskExecutor getServerTaskExecutor()
	{
		return serverTaskExecutor;
	}

	public RestAction<List<InstalledServer>> retrieveInstalledServers()
	{
		return new RestAction<>(() -> rest.get("/apps/@me").thenApply(response -> {
			JsonObject data = EntityJson.requireObject(response);
			var entries = EntityJson.requireArray(data.get("servers"));
			var servers = new java.util.ArrayList<InstalledServer>(entries.size());
			for (JsonElement entry : entries)
				servers.add(InstalledServer.fromJson(EntityJson.requireObject(entry)));
			return List.copyOf(servers);
		}));
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
		serverTaskExecutor.close();
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
