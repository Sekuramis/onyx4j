package de.onyx4j.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.onyx4j.api.entities.channel.ServerChannel;
import de.onyx4j.api.entities.channel.TextChannel;
import de.onyx4j.api.entities.channel.VoiceChannel;
import de.onyx4j.api.entities.server.Server;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.events.GatewayEvent;
import de.onyx4j.api.events.OnyxListenerAdapter;
import de.onyx4j.api.events.ReadyEvent;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.events.member.MemberJoinEvent;
import de.onyx4j.api.events.message.MessageReceivedEvent;
import de.onyx4j.api.gateway.OnyxGateway;
import de.onyx4j.api.requests.CommandUpdateAction;
import de.onyx4j.api.requests.OnyxRestClient;
import de.onyx4j.api.requests.RestAction;
import de.onyx4j.api.events.interaction.ComponentInteractionEvent;
import de.onyx4j.api.events.interaction.ModalInteractionEvent;
import de.onyx4j.api.events.interaction.AutocompleteInteractionEvent;
import de.onyx4j.api.interactions.commands.CommandRegistry;
import de.onyx4j.api.events.interaction.InteractionAutoDefer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
	private final InteractionAutoDefer autoDefer = new InteractionAutoDefer(2500);

	private volatile boolean connected;
	private volatile boolean everReady;
	private volatile boolean shuttingDown;

	OnyxClient(String token, String baseUrl, List<OnyxListenerAdapter> listeners)
	{
		this.rest = new OnyxRestClient(token, baseUrl);
		this.commandRegistry = new CommandRegistry(rest);
		this.listeners = new CopyOnWriteArrayList<>(listeners);
		this.ready = new CompletableFuture<>();
		this.started = new AtomicBoolean();

		this.gateway = new OnyxGateway(baseUrl, token, this::handleReady, this::handleGatewayEvent,
				this::handleInteraction, this::handleDisconnect, this::handleError);
	}

	public void connect()
	{
		if (!started.compareAndSet(false, true))
		{
			return;
		}

		rest.get("/apps/@me").thenCompose(result -> commandRegistry.syncIfEnabled()).whenComplete((result, error) ->
		{
			if (error != null)
			{
				ready.completeExceptionally(error);
				handleError(error);
				return;
			}

			try
			{
				gateway.connect();
			} catch (Exception exception)
			{
				ready.completeExceptionally(exception);
				handleError(exception);
			}
		});
	}

	private void handleReady(JsonObject data)
	{
		if (shuttingDown)
		{
			return;
		}

		boolean reconnect = everReady;
		everReady = true;
		connected = true;

		ReadyEvent event = new ReadyEvent(this, reconnect, data);

		ready.complete(this);
		dispatch(listener -> listener.onReady(event));
	}

	private void handleGatewayEvent(JsonObject payload)
	{
		if (shuttingDown || !payload.has("type"))
		{
			return;
		}

		GatewayEvent event = new GatewayEvent(this, payload);
		String type = event.getType();

		dispatch(listener -> listener.onGatewayEvent(event));

		if (type == null)
		{
			return;
		}

		switch (type)
		{
		case "message.created" ->
		{
			MessageReceivedEvent messageEvent = new MessageReceivedEvent(this, payload);

			dispatch(listener -> listener.onMessageReceived(messageEvent));
		}

		case "member.joined" ->
		{
			MemberJoinEvent memberEvent = new MemberJoinEvent(this, payload);

			dispatch(listener -> listener.onMemberJoin(memberEvent));
		}

		default ->
		{
		}
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

			autoDefer.schedule(
					event::isAcknowledged,
					() -> event.deferReply().queue(),
					this::handleError
			);


			dispatch(listener -> listener.onComponentInteraction(event));
		}

		case "modal" ->
		{
			ModalInteractionEvent event = new ModalInteractionEvent(this, payload);

			autoDefer.schedule(
					event::isAcknowledged,
					() -> event.deferReply().queue(),
					this::handleError
			);

			dispatch(listener -> listener.onModalInteraction(event));
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

			dispatch(listener -> listener.onSlashCommandInteraction(event));
		}

		case "autocomplete" ->
		{
			AutocompleteInteractionEvent event = new AutocompleteInteractionEvent(this, payload);

			autoDefer.schedule(
					event::isAcknowledged,
					() -> event.deferReply().queue(),
					this::handleError
			);

			dispatch(listener -> listener.onAutocompleteInteraction(event));
		}

		default ->
		{
		}
		}
	}

	private void handleDisconnect(String reason)
	{
		connected = false;

		if (!shuttingDown)
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

	public VoiceChannel getVoiceChannelById(String channelId)
	{
		return new VoiceChannel(this, channelId);
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
		shuttingDown = true;
		connected = false;
		started.set(false);

		if (!ready.isDone())
		{
			ready.completeExceptionally(new IllegalStateException("Client was shut down"));
		}

		gateway.disconnect();
		autoDefer.close();
	}

	public CommandRegistry getCommandRegistry()
	{
		return commandRegistry;
	}

}