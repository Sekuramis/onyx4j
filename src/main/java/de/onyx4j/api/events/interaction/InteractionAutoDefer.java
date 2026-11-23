package de.onyx4j.api.events.interaction;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
/**
 * © Copyright 24.11.2026 - 00:05 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class InteractionAutoDefer implements AutoCloseable {

	private final ScheduledExecutorService scheduler;
	private final long delayMillis;

	public InteractionAutoDefer(long delayMillis) {
		if (delayMillis < 0 || delayMillis > 2900) {
			throw new IllegalArgumentException(
					"Auto-defer delay must be between 0 and 2900 ms"
			);
		}

		this.delayMillis = delayMillis;
		this.scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
			Thread thread = new Thread(task, "onyx4j-interaction-defer");
			thread.setDaemon(true);
			return thread;
		});
	}

	public void schedule(
			BooleanSupplier acknowledged,
			Supplier<CompletableFuture<Void>> defer,
			Consumer<Throwable> errorHandler
	) {
		Objects.requireNonNull(acknowledged, "acknowledged");
		Objects.requireNonNull(defer, "defer");
		Objects.requireNonNull(errorHandler, "errorHandler");

		if (delayMillis == 0) {
			return;
		}

		scheduler.schedule(() -> {
			try {
				if (acknowledged.getAsBoolean()) {
					return;
				}

				defer.get().whenComplete((result, error) -> {
					if (error != null) {
						errorHandler.accept(error);
					}
				});
			} catch (Throwable error) {
				errorHandler.accept(error);
			}
		}, delayMillis, TimeUnit.MILLISECONDS);
	}

	@Override
	public void close() {
		scheduler.shutdownNow();
	}
}