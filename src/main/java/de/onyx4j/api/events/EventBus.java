package de.onyx4j.api.events;

import de.onyx4j.api.events.annotation.SubscribeEvent;
import de.onyx4j.api.events.gateway.EventSubscription;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * © Copyright 24.11.2026 - 00:53 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class EventBus implements AutoCloseable {

	private final Consumer<Throwable> errorHandler;
	private final ThreadPoolExecutor executor;
	private final Map<Object, Group> groups = new IdentityHashMap<>();
	private volatile List<Registration> registrations = List.of();
	private volatile boolean closed;

	public EventBus(Consumer<Throwable> errorHandler) {
		this(errorHandler, 2, 1024);
	}

	public EventBus(Consumer<Throwable> errorHandler, int workers, int queueCapacity) {
		this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler");
		if (workers < 1 || queueCapacity < 1)
			throw new IllegalArgumentException("Workers and queue capacity must be positive");
		executor = new ThreadPoolExecutor(workers, workers, 30, TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(queueCapacity), task -> {
					Thread thread = new Thread(task, "onyx4j-event-worker");
					thread.setDaemon(true);
					return thread;
				});
		executor.allowCoreThreadTimeOut(true);
	}

	public synchronized EventSubscription register(Object listener) {
		requireOpen();
		Objects.requireNonNull(listener, "listener");
		if (groups.containsKey(listener))
			throw new IllegalArgumentException("Listener instance is already registered");

		Method[] methods = listener.getClass().getDeclaredMethods();
		Arrays.sort(methods, Comparator.comparing(Method::toGenericString));
		List<Registration> additions = new ArrayList<>();
		for (Method method : methods) {
			SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
			if (annotation == null || method.isSynthetic() || method.isBridge()) continue;
			if (Modifier.isStatic(method.getModifiers()) || Modifier.isAbstract(method.getModifiers())
					|| method.isVarArgs() || method.getReturnType() != void.class || method.getParameterCount() != 1
					|| method.getParameterTypes()[0].isPrimitive() || !method.trySetAccessible()) {
				throw new IllegalArgumentException("Invalid event handler: " + method);
			}
			additions.add(new Registration(method.getParameterTypes()[0], annotation.priority(),
					annotation.async(), event -> invoke(listener, method, event)));
		}
		if (additions.isEmpty())
			throw new IllegalArgumentException("No declared @SubscribeEvent methods in " + listener.getClass().getName());
		Group group = new Group(listener, List.copyOf(additions));
		groups.put(listener, group);
		add(additions);
		return group;
	}

	public synchronized boolean unregister(Object listener) {
		Group group = groups.get(listener);
		return group != null && group.unsubscribe();
	}

	public <T> EventSubscription on(Class<T> eventType, Consumer<? super T> handler) {
		return on(eventType, EventPriority.NORMAL, false, handler);
	}

	public synchronized <T> EventSubscription on(Class<T> eventType, EventPriority priority,
			boolean async, Consumer<? super T> handler) {
		requireOpen();
		Objects.requireNonNull(eventType, "eventType");
		Objects.requireNonNull(handler, "handler");
		if (eventType.isPrimitive()) throw new IllegalArgumentException("Event type must be a reference type");
		Registration registration = new Registration(eventType, Objects.requireNonNull(priority, "priority"),
				async, event -> handler.accept(eventType.cast(event)));
		add(List.of(registration));
		return registration;
	}

	public void publish(Object event) {
		Objects.requireNonNull(event, "event");
		if (closed) return;
		for (Registration registration : registrations) {
			if (closed) return;
			if (!registration.isActive() || !registration.type.isInstance(event)) continue;
			if (registration.async) {
				try {
					executor.execute(() -> registration.deliver(event));
				} catch (RejectedExecutionException exception) {
					if (!closed) report(exception);
				}
			} else {
				registration.deliver(event);
			}
		}
	}

	public int getListenerCount() {
		return registrations.size();
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public synchronized void close() {
		if (closed) return;
		closed = true;
		for (Registration registration : registrations) registration.active.set(false);
		for (Group group : groups.values()) group.active.set(false);
		registrations = List.of();
		groups.clear();
		executor.shutdownNow();
	}

	private void requireOpen() {
		if (closed) throw new IllegalStateException("Event bus is closed");
	}

	private void add(List<Registration> additions) {
		List<Registration> next = new ArrayList<>(registrations);
		next.addAll(additions);
		next.sort(Comparator.comparing(registration -> registration.priority));
		registrations = List.copyOf(next);
	}

	private synchronized void remove(List<Registration> removed) {
		List<Registration> next = new ArrayList<>(registrations);
		next.removeAll(removed);
		registrations = List.copyOf(next);
	}

	private static void invoke(Object listener, Method method, Object event) {
		try {
			method.invoke(listener, event);
		} catch (InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof RuntimeException runtime) throw runtime;
			if (cause instanceof Error error) throw error;
			throw new IllegalStateException("Event handler failed: " + method, cause);
		} catch (IllegalAccessException exception) {
			throw new IllegalStateException("Cannot invoke event handler: " + method, exception);
		}
	}

	private void report(Throwable error) {
		try {
			errorHandler.accept(error);
		} catch (RuntimeException exception) {
			exception.printStackTrace(System.err);
		}
	}

	private final class Registration implements EventSubscription {
		private final Class<?> type;
		private final EventPriority priority;
		private final boolean async;
		private final Consumer<Object> handler;
		private final AtomicBoolean active = new AtomicBoolean(true);

		private Registration(Class<?> type, EventPriority priority, boolean async, Consumer<Object> handler) {
			this.type = type;
			this.priority = priority;
			this.async = async;
			this.handler = handler;
		}

		private void deliver(Object event) {
			if (closed || !active.get()) return;
			try {
				handler.accept(event);
			} catch (RuntimeException exception) {
				report(exception);
			}
		}

		@Override
		public boolean unsubscribe() {
			if (!active.compareAndSet(true, false)) return false;
			remove(List.of(this));
			return true;
		}

		@Override
		public boolean isActive() {
			return active.get();
		}
	}

	private final class Group implements EventSubscription {
		private final Object listener;
		private final List<Registration> entries;
		private final AtomicBoolean active = new AtomicBoolean(true);

		private Group(Object listener, List<Registration> entries) {
			this.listener = listener;
			this.entries = entries;
		}

		@Override
		public boolean unsubscribe() {
			synchronized (EventBus.this) {
				if (!active.compareAndSet(true, false)) return false;
				for (Registration registration : entries) registration.active.set(false);
				remove(entries);
				groups.remove(listener);
				return true;
			}
		}

		@Override
		public boolean isActive() {
			return active.get();
		}
	}
}
