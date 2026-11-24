package de.onyx4j.api.requests;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.onyx4j.api.exceptions.OnyxApiException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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

    private static final int MAX_RETRIES = 3;

    private final HttpClient client;
    private final String token;
    private final String baseUrl;

    public OnyxRestClient(String token, String baseUrl) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Onyx bot token must not be blank");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Onyx base URL must not be blank");
        }

        this.token = token;
        this.baseUrl = baseUrl.replaceAll("/+$", "") + "/api/v1";
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public CompletableFuture<JsonElement> request(String method, String path, JsonElement body) {
        if (method == null || method.isBlank() || path == null || path.isBlank()) {
            throw new IllegalArgumentException("HTTP method and path must be provided");
        }
        return execute(method.toUpperCase(java.util.Locale.ROOT), path, body, 0);
    }

    private CompletableFuture<JsonElement> execute(String method, String path, JsonElement body, int attempt) {
        String url = baseUrl + (path.startsWith("/") ? path : "/" + path);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Authorization", "Bot " + token)
                .header("User-Agent", "onyx4j/0.11.0");

        if (body != null) {
            builder.header("Content-Type", "application/json");
        }

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8);

        HttpRequest request = builder.method(method, publisher).build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .handle((response, exception) -> {
                    if (exception != null) {
                        if (attempt < MAX_RETRIES && method.equals("GET")) {
                            return retryLater(method, path, body, attempt, backoff(attempt));
                        }
                        return CompletableFuture.<JsonElement>failedFuture(
                                new OnyxApiException(0, method, path, null, unwrap(exception))
                        );
                    }

                    int status = response.statusCode();

                    if (status == 429 && attempt < MAX_RETRIES) {
                        long delay = retryAfter(response);
                        if (delay <= 60_000) {
                            return retryLater(method, path, body, attempt, delay + 50);
                        }
                    }

                    if (method.equals("GET") && attempt < MAX_RETRIES
                            && (status == 502 || status == 503 || status == 504)) {
                        return retryLater(method, path, body, attempt, backoff(attempt));
                    }

                    if (status < 200 || status >= 300) {
                        return CompletableFuture.<JsonElement>failedFuture(
                                new OnyxApiException(status, method, path, response.body())
                        );
                    }

                    String data = response.body();
                    if (data == null || data.isBlank()) {
                        return CompletableFuture.<JsonElement>completedFuture(JsonNull.INSTANCE);
                    }

                    try {
                        return CompletableFuture.completedFuture(JsonParser.parseString(data));
                    } catch (RuntimeException ignored) {
                        return CompletableFuture.<JsonElement>completedFuture(new JsonPrimitive(data));
                    }
                })
                .thenCompose(Function.identity());
    }

    private CompletableFuture<JsonElement> retryLater(
            String method, String path, JsonElement body, int attempt, long delay
    ) {
        return CompletableFuture.supplyAsync(
                () -> (Void) null,
                CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS)
        ).thenCompose(ignored -> execute(method, path, body, attempt + 1));
    }

    private static long backoff(int attempt) {
        return Math.min(8000, 500L * (1L << attempt));
    }

    private static long retryAfter(HttpResponse<?> response) {
        String header = response.headers().firstValue("Retry-After").orElse(null);
        if (header == null || header.isBlank()) {
            return 1000;
        }

        try {
            return Math.max(0, (long) (Double.parseDouble(header) * 1000));
        } catch (NumberFormatException ignored) {
            try {
                long date = ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant().toEpochMilli();
                return Math.max(0, date - System.currentTimeMillis());
            } catch (RuntimeException ignoredDate) {
                return 1000;
            }
        }
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof CompletionException && error.getCause() != null
                ? error.getCause() : error;
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
