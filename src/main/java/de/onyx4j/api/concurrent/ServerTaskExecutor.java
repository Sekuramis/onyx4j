package de.onyx4j.api.concurrent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * © Copyright 24.11.2026 - 00:53 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ServerTaskExecutor implements AutoCloseable {

	private final Object lock = new Object();
	private final List<ThreadPoolExecutor> partitions;
	private final Set<Task<?>> outstanding = new HashSet<>();
	private final LongAdder accepted = new LongAdder();
	private final LongAdder successful = new LongAdder();
	private final LongAdder failed = new LongAdder();
	private final LongAdder rejected = new LongAdder();
	private volatile boolean closed;

	public ServerTaskExecutor(ServerTaskOptions options) {
		Objects.requireNonNull(options, "options");
		List<ThreadPoolExecutor> workers = new ArrayList<>();
		for (int i = 0; i < options.partitions(); i++) {
			int partition = i;
			ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
					new ArrayBlockingQueue<>(options.queueCapacity()), runnable -> {
						Thread thread = new Thread(runnable, "onyx4j-server-worker-" + partition);
						thread.setDaemon(true);
						return thread;
					});
			executor.allowCoreThreadTimeOut(true);
			workers.add(executor);
		}
		partitions = List.copyOf(workers);
	}

	public int getPartition(String serverId) {
		if (serverId == null || serverId.isBlank())
			throw new IllegalArgumentException("Server ID is required");
		return Math.floorMod(serverId.hashCode(), partitions.size());
	}

	public <T> CompletableFuture<T> submit(String serverId, Callable<T> callable) {
		ThreadPoolExecutor executor = partitions.get(getPartition(serverId));
		Objects.requireNonNull(callable, "callable");
		Task<T> task = new Task<>(callable);
		synchronized (lock) {
			if (closed) {
				rejected.increment();
				return CompletableFuture.failedFuture(new RejectedExecutionException("Server task executor is closed"));
			}
			outstanding.add(task);
			try {
				executor.execute(task);
			} catch (RejectedExecutionException exception) {
				outstanding.remove(task);
				rejected.increment();
				return CompletableFuture.failedFuture(exception);
			}
			accepted.increment();
			task.future.whenComplete((value, error) -> {
				if (error == null) successful.increment();
				else failed.increment();
				synchronized (lock) {
					outstanding.remove(task);
				}
				if (task.future.isCancelled()) executor.remove(task);
			});
		}
		return task.future;
	}

	public ServerTaskStatistics getStatistics() {
		int queued = partitions.stream().mapToInt(executor -> executor.getQueue().size()).sum();
		int running = partitions.stream().mapToInt(ThreadPoolExecutor::getActiveCount).sum();
		return new ServerTaskStatistics(accepted.sum(), successful.sum(), failed.sum(), rejected.sum(),
				queued, running, partitions.size());
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		List<Task<?>> pending;
		synchronized (lock) {
			if (closed) return;
			closed = true;
			pending = List.copyOf(outstanding);
			for (ThreadPoolExecutor executor : partitions) executor.shutdownNow();
		}
		for (Task<?> task : pending)
			task.future.completeExceptionally(new RejectedExecutionException("Server task executor was shut down"));
	}

	private final class Task<T> implements Runnable {
		private final Callable<T> callable;
		private final CompletableFuture<T> future = new CompletableFuture<>();

		private Task(Callable<T> callable) {
			this.callable = callable;
		}

		@Override
		public void run() {
			if (future.isDone()) return;
			if (closed) {
				future.completeExceptionally(new RejectedExecutionException("Server task executor was shut down"));
				return;
			}
			try {
				future.complete(callable.call());
			} catch (Throwable error) {
				future.completeExceptionally(error);
				if (error instanceof InterruptedException) Thread.currentThread().interrupt();
				if (error instanceof Error fatal) throw fatal;
			}
		}
	}
}
