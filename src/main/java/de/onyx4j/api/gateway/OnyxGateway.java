package de.onyx4j.api.gateway;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.engineio.client.transports.WebSocket;


import java.net.URI;
import java.util.Map;
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

    private Socket socket;

    public OnyxGateway(
            String baseUrl,
            String token,
            Consumer<JsonObject> readyHandler,
            Consumer<JsonObject> eventHandler,
            Consumer<JsonObject> interactionHandler,
            Consumer<String> disconnectHandler,
            Consumer<Throwable> errorHandler
    ) {
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
                .setAuth(Map.of("token", token))
                .setPath("/socket.io")
                .setTransports(new String[]{WebSocket.NAME})
                .setUpgrade(false)
                .setReconnection(true)
                .setReconnectionDelay(1000)
                .setReconnectionDelayMax(30000)
                .setTimeout(20000)
                .build();

        socket = IO.socket(URI.create(baseUrl), options);

        socket.on("ready", args ->
                handleJson(args, readyHandler));

        socket.on("event", args ->
                handleJson(args, eventHandler));

        socket.on("interaction:create", args ->
                handleJson(args, interactionHandler));

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            String reason = args.length > 0
                    ? String.valueOf(args[0])
                    : "Unknown";

            disconnectHandler.accept(reason);
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
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

        socket.connect();
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
        if (socket != null) {
            socket.disconnect();
            socket.off();
            socket = null;
        }
    }

    public boolean isConnected() {
        Socket current = socket;
        return current != null && current.connected();
    }
}