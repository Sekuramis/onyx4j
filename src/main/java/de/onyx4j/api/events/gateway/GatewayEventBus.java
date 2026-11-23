package de.onyx4j.api.events.gateway;

import de.onyx4j.api.events.GatewayEvent;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * © Copyright 24.11.2026 - 00:53 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class GatewayEventBus implements AutoCloseable {

	private final ConcurrentHashMap<String, CopyOnWriteArrayList<Registration>> subscriptions =
			new ConcurrentHashMap<>();
	private final Set<CompletableFuture<GatewayEvent>> waiters = ConcurrentHashMap.newKeySet();
	private final AtomicBoolean closed = new AtomicBoolean();
	private final Consumer<Throwable> errorHandler;

	public GatewayEventBus(Consumer<Throwable> errorHandler) {
		this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
	}

	public EventSubscription on(String type, Consumer<GatewayEvent> handler) {
		return subscribe(type, handler, false);
	}

	public EventSubscription once(String type, Consumer<GatewayEvent> handler) {
		return subscribe(type, handler, true);
	}

	public CompletableFuture<GatewayEvent> waitFor(String type, Duration timeout) {
		return waitFor(type, event -> true, timeout);
	}

	public CompletableFuture<GatewayEvent> waitFor(
			String type,
			Predicate<GatewayEvent> predicate,
			Duration timeout
	) {
		validateType(type);
		Objects.requireNonNull(predicate, "predicate");
		Objects.requireNonNull(timeout, "timeout");

		if (timeout.isNegative()) {
			throw new IllegalArgumentException("Timeout must not be negative");
		}
		if (closed.get()) {
			return CompletableFuture.failedFuture(new IllegalStateException("Event bus is closed"));
		}

		CompletableFuture<GatewayEvent> future = new CompletableFuture<>();
		waiters.add(future);

		EventSubscription subscription;
		try {
			subscription = on(type, event -> {
				try {
					if (predicate.test(event)) {
						future.complete(event);
					}
				} catch (RuntimeException exception) {
					future.completeExceptionally(exception);
				}
			});
		} catch (IllegalStateException exception) {
			waiters.remove(future);
			future.completeExceptionally(exception);
			return future;
		}

		future.whenComplete((event, failure) -> {
			subscription.unsubscribe();
			waiters.remove(future);
		});

		if (closed.get()) {
			future.completeExceptionally(new IllegalStateException("Event bus is closed"));
		}

		if (!timeout.isZero() && !future.isDone()) {
			future.orTimeout(Math.max(1, timeout.toMillis()), TimeUnit.MILLISECONDS);
		}

		return future;
	}

	public void publish(GatewayEvent event) {
		Objects.requireNonNull(event, "event");
		if (closed.get()) {
			return;
		}

		String type = Objects.requireNonNull(event.getType(), "event.type");
		publishTo(type, event);

		if (!GatewayEventTypes.ALL.equals(type)) {
			publishTo(GatewayEventTypes.ALL, event);
		}
	}

	public int getListenerCount(String type) {
		validateType(type);
		CopyOnWriteArrayList<Registration> registrations = subscriptions.get(type);
		return registrations == null ? 0 : registrations.size();
	}

	public boolean isClosed() {
		return closed.get();
	}

	@Override
	public void close() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}

		for (CompletableFuture<GatewayEvent> waiter : waiters) {
			waiter.completeExceptionally(new IllegalStateException("Event bus is closed"));
		}

		subscriptions.values().forEach(registrations ->
				registrations.forEach(Registration::unsubscribe));
		subscriptions.clear();
		waiters.clear();
	}

	private EventSubscription subscribe(String type, Consumer<GatewayEvent> handler, boolean once) {
		validateType(type);
		Objects.requireNonNull(handler, "handler");

		if (closed.get()) {
			throw new IllegalStateException("Event bus is closed");
		}

		Registration registration = new Registration(type, handler, once);
		subscriptions.compute(type, (key, registrations) -> {
			CopyOnWriteArrayList<Registration> current = registrations == null
					? new CopyOnWriteArrayList<>()
					: registrations;
			current.add(registration);
			return current;
		});

		if (closed.get()) {
			registration.unsubscribe();
			throw new IllegalStateException("Event bus is closed");
		}

		return registration;
	}

	private void publishTo(String type, GatewayEvent event) {
		CopyOnWriteArrayList<Registration> registrations = subscriptions.get(type);
		if (registrations == null) {
			return;
		}

		for (Registration registration : registrations) {
			registration.deliver(event);
		}
	}

	private void remove(Registration registration) {
		subscriptions.computeIfPresent(registration.type, (type, registrations) -> {
			registrations.remove(registration);
			return registrations.isEmpty() ? null : registrations;
		});
	}

	private void report(Throwable failure) {
		try {
			errorHandler.accept(failure);
		} catch (RuntimeException handlerError) {
			handlerError.printStackTrace(System.err);
		}
	}

	private static void validateType(String type) {
		if (type == null || type.isBlank()) {
			throw new IllegalArgumentException("Event type must not be blank");
		}
	}

	private final class Registration implements EventSubscription {

		private final String type;
		private final Consumer<GatewayEvent> handler;
		private final boolean once;
		private final AtomicBoolean active = new AtomicBoolean(true);

		private Registration(String type, Consumer<GatewayEvent> handler, boolean once) {
			this.type = type;
			this.handler = handler;
			this.once = once;
		}

		private void deliver(GatewayEvent event) {
			if (once) {
				if (!active.compareAndSet(true, false)) {
					return;
				}
				remove(this);
			} else if (!active.get()) {
				return;
			}

			try {
				handler.accept(event);
			} catch (RuntimeException exception) {
				report(exception);
			}
		}

		@Override
		public boolean unsubscribe() {
			if (!active.compareAndSet(true, false)) {
				return false;
			}
			remove(this);
			return true;
		}

		@Override
		public boolean isActive() {
			return active.get();
		}
	}
}