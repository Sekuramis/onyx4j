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
        this.action = action;
    }

    public CompletableFuture<T> queue() {
        return action.get();
    }

    public void queue(Consumer<T> success, Consumer<Throwable> failure) {
        action.get().whenComplete((result, error) -> {
            if (error != null) {
                failure.accept(error);
            } else {
                success.accept(result);
            }
        });
    }

    public <R> RestAction<R> map(Function<? super T, ? extends R> mapper) {
        return new RestAction<>(() -> action.get().thenApply(mapper));
    }

    public T complete() {
        return action.get().join();
    }
}