package de.onyx4j.api.requests;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
public final class RestAction<T> {

    private final Supplier<CompletableFuture<T>> action;

    public RestAction(Supplier<CompletableFuture<T>> action) {
        this.action = java.util.Objects.requireNonNull(action, "action");
    }

    public CompletableFuture<T> queue() {
        try {
            return java.util.Objects.requireNonNull(action.get(), "action returned null");
        } catch (RuntimeException error) {
            return CompletableFuture.failedFuture(error);
        }
    }

    public void queue(Consumer<T> success, Consumer<Throwable> failure) {
        java.util.Objects.requireNonNull(success, "success");
        java.util.Objects.requireNonNull(failure, "failure");
        queue().whenComplete((result, error) -> {
            if (error != null) {
                failure.accept(error);
            } else {
                success.accept(result);
            }
        });
    }

    public <R> RestAction<R> map(Function<? super T, ? extends R> mapper) {
        java.util.Objects.requireNonNull(mapper, "mapper");
        return new RestAction<>(() -> queue().thenApply(mapper));
    }

    public T complete() {
        return queue().join();
    }

    public <R> RestAction<R> flatMap(Function<? super T, RestAction<R>> mapper) {
        java.util.Objects.requireNonNull(mapper, "mapper");
        return new RestAction<>(() -> queue().thenCompose(value -> mapper.apply(value).queue()));
    }
}
