package de.onyx4j.api.gateway;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.client.Manager;
import io.socket.engineio.client.Transport;
import io.socket.engineio.parser.Packet;
import io.socket.engineio.client.transports.WebSocket;


import java.net.URI;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
/**
 * © Copyright 23.11.2026 - 17:10 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class OnyxGateway {

    private final String baseUrl;
    private final String token;
    private final Consumer<JsonObject> readyHandler;
    private final Consumer<JsonObject> eventHandler;
    private final Consumer<JsonObject> interactionHandler;
    private final Consumer<String> disconnectHandler;
    private final Consumer<Throwable> errorHandler;

    private final GatewayStatus status;
    private volatile Socket socket;
    private ScheduledExecutorService reconnectExecutor;
    private ScheduledFuture<?> reconnectTask;
    private int reconnectFailures;

    public OnyxGateway(
            String baseUrl,
            String token,
            Consumer<JsonObject> readyHandler,
            Consumer<JsonObject> eventHandler,
            Consumer<JsonObject> interactionHandler,
            Consumer<String> disconnectHandler,
            Consumer<Throwable> errorHandler
    ) {
        this(baseUrl, token, readyHandler, eventHandler, interactionHandler, disconnectHandler,
                errorHandler, new GatewayStatus());
    }

    public OnyxGateway(
            String baseUrl,
            String token,
            Consumer<JsonObject> readyHandler,
            Consumer<JsonObject> eventHandler,
            Consumer<JsonObject> interactionHandler,
            Consumer<String> disconnectHandler,
            Consumer<Throwable> errorHandler,
            GatewayStatus status
    ) {
        this.status = java.util.Objects.requireNonNull(status, "status");
        this.baseUrl = baseUrl;
        this.token = token;
        this.readyHandler = readyHandler;
        this.eventHandler = eventHandler;
        this.interactionHandler = interactionHandler;
        this.disconnectHandler = disconnectHandler;
        this.errorHandler = errorHandler;
    }

    public synchronized void connect() {
        if (socket != null) {
            return;
        }

        IO.Options options = IO.Options.builder()
                .setForceNew(true)
                .setAuth(Map.of("token", token))
                .setPath("/socket.io")
                .setTransports(new String[]{WebSocket.NAME})
                .setUpgrade(false)
                .setReconnection(true)
                .setReconnectionDelay(1000)
                .setReconnectionDelayMax(30000)
                .setTimeout(20000)
                .build();

        Socket current = IO.socket(URI.create(baseUrl), options);
        socket = current;
        status.markConnecting();
        reconnectExecutor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "onyx4j-gateway-reconnect");
            thread.setDaemon(true);
            return thread;
        });

        current.io().on(Manager.EVENT_RECONNECT_ATTEMPT, args -> {
            if (socket == current) status.markReconnectAttempt();
        });
        current.io().on(Manager.EVENT_TRANSPORT, args -> {
            if (socket != current || args.length == 0 || !(args[0] instanceof Transport transport)) return;
            transport.on(Transport.EVENT_PACKET, packets -> {
                if (socket == current && packets.length > 0 && packets[0] instanceof Packet<?> packet) {
                    status.markPacket(Packet.PING.equals(packet.type) || Packet.PONG.equals(packet.type));
                }
            });
        });

        current.on("ready", args -> {
            if (socket == current) {
                resetReconnect(current);
                handleJson(args, readyHandler);
            }
        });

        current.on("event", args -> {
            if (socket == current) handleJson(args, eventHandler);
        });

        current.on("interaction:create", args -> {
            if (socket == current) handleJson(args, interactionHandler);
        });

        current.on(Socket.EVENT_DISCONNECT, args -> {
            if (socket != current) return;
            String reason = args.length > 0
                    ? String.valueOf(args[0])
                    : "Unknown";

            disconnectHandler.accept(reason);
            if ("io server disconnect".equals(reason)) scheduleReconnect(current, false);
        });

        current.on(Socket.EVENT_CONNECT_ERROR, args -> {
            if (socket != current) return;
            status.markConnectionError();
            if (!current.isActive()) scheduleReconnect(current, true);
            if (args.length == 0 || args[0] == null) {
                errorHandler.accept(
                        new IllegalStateException("Unknown gateway connection error")
                );
                return;
            }

            Object cause = args[0];

            if (cause instanceof Throwable throwable) {
                errorHandler.accept(throwable);
            } else {
                errorHandler.accept(
                        new IllegalStateException(String.valueOf(cause))
                );
            }
        });

        current.connect();
    }

    private synchronized void resetReconnect(Socket current) {
        if (socket != current) return;
        reconnectFailures = 0;
        if (reconnectTask != null) reconnectTask.cancel(false);
        reconnectTask = null;
    }

    private synchronized void scheduleReconnect(Socket current, boolean slow) {
        if (socket != current || reconnectExecutor == null || reconnectExecutor.isShutdown()
                || (reconnectTask != null && !reconnectTask.isDone())) return;
        long delay = Math.min(30_000, (slow ? 5000L : 1000L) * (1L << Math.min(5, reconnectFailures++)));
        reconnectTask = reconnectExecutor.schedule(() -> {
            synchronized (OnyxGateway.this) {
                reconnectTask = null;
                if (socket != current || current.connected() || current.isActive()) return;
                status.markReconnectAttempt();
                current.connect();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private void handleJson(
            Object[] args,
            Consumer<JsonObject> handler
    ) {
        try {
            if (args.length == 0 || args[0] == null) {
                return;
            }

            JsonObject payload = JsonParser
                    .parseString(args[0].toString())
                    .getAsJsonObject();

            handler.accept(payload);
        } catch (Exception exception) {
            errorHandler.accept(exception);
        }
    }

    public synchronized void disconnect() {
        Socket current = socket;
        socket = null;
        if (reconnectTask != null) reconnectTask.cancel(false);
        reconnectTask = null;
        if (reconnectExecutor != null) reconnectExecutor.shutdownNow();
        reconnectExecutor = null;
        reconnectFailures = 0;
        if (current != null) {
            current.io().reconnection(false);
            current.disconnect();
            current.off();
        }
    }

    public boolean isConnected() {
        Socket current = socket;
        return current != null && current.connected();
    }
}
