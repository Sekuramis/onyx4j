package de.onyx4j.api.gateway;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
/**
 * © Copyright 24.11.2026 - 00:32 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class GatewayStatus {

	public enum State {
		DISCONNECTED, CONNECTING, READY, SHUTDOWN
	}

	private final AtomicLong connectionCount = new AtomicLong();
	private volatile State state = State.DISCONNECTED;
	private volatile Instant lastReadyAt;
	private volatile Instant lastDisconnectedAt;
	private volatile String lastDisconnectReason;

	public State getState() {
		return state;
	}

	public boolean isReady() {
		return state == State.READY;
	}

	public long getConnectionCount() {
		return connectionCount.get();
	}

	public long getReconnectCount() {
		return Math.max(0L, connectionCount.get() - 1L);
	}

	public Instant getLastReadyAt() {
		return lastReadyAt;
	}

	public Instant getLastDisconnectedAt() {
		return lastDisconnectedAt;
	}

	public String getLastDisconnectReason() {
		return lastDisconnectReason;
	}

	public synchronized void markConnecting() {
		if (state != State.SHUTDOWN) {
			state = State.CONNECTING;
		}
	}

	public synchronized void markReady() {
		if (state == State.SHUTDOWN) {
			return;
		}
		if (state != State.READY) {
			connectionCount.incrementAndGet();
		}
		lastReadyAt = Instant.now();
		state = State.READY;
	}

	public synchronized void markDisconnected(String reason) {
		if (state == State.SHUTDOWN) {
			return;
		}
		lastDisconnectedAt = Instant.now();
		lastDisconnectReason = reason;
		state = State.DISCONNECTED;
	}

	public synchronized void markShutdown() {
		state = State.SHUTDOWN;
	}
}