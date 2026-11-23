package de.onyx4j.api.requests;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
/**
 * © Copyright 23.11.2026 - 16:51 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class OnyxRestClient {

    private final HttpClient client;
    private final String token;
    private final String baseUrl;

    public OnyxRestClient(String token, String baseUrl) {
        this.token = token;
        this.baseUrl = baseUrl.replaceAll("/+$", "") + "/api/v1";
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public CompletableFuture<JsonElement> request(
            String method,
            String path,
            JsonElement body
    ) {
        String url = baseUrl + (path.startsWith("/") ? path : "/" + path);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Authorization", "Bot " + token)
                .header("User-Agent", "onyx4j/0.1.0");

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body.toString());

        builder.method(method.toUpperCase(), publisher);

        return client.sendAsync(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenApply(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Onyx API HTTP " + response.statusCode()
                                + ": " + response.body()
                );
            }

            String result = response.body();

            if (result == null || result.isBlank()) {
                return JsonNull.INSTANCE;
            }

            return JsonParser.parseString(result);
        });
    }

    public CompletableFuture<JsonElement> get(String path) {
        return request("GET", path, null);
    }

    public CompletableFuture<JsonElement> post(String path, JsonElement body) {
        return request("POST", path, body);
    }

    public CompletableFuture<JsonElement> put(String path, JsonElement body) {
        return request("PUT", path, body);
    }

    public CompletableFuture<JsonElement> patch(String path, JsonElement body) {
        return request("PATCH", path, body);
    }

    public CompletableFuture<JsonElement> delete(String path) {
        return request("DELETE", path, null);
    }
}