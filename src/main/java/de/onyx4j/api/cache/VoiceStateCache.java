package de.onyx4j.api.cache;

import de.onyx4j.api.entities.voice.VoiceState;
import de.onyx4j.api.events.GatewayEvent;
import de.onyx4j.api.events.voice.VoiceJoinEvent;
import de.onyx4j.api.events.voice.VoiceLeaveEvent;
import de.onyx4j.api.internal.EntityJson;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * © Copyright 23.11.2026 - 17:11 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class VoiceStateCache {

	private record Key(String serverId, String userId) {}

	private final Map<Key, VoiceState> states = new HashMap<>();

	public synchronized void apply(GatewayEvent event) {
		String serverId = event.getServerId();
		if (serverId == null || serverId.isBlank()) return;

		if (event instanceof VoiceJoinEvent join) {
			Key key = new Key(serverId, join.getUserId());
			VoiceState previous = states.get(key);
			if (previous != null && previous.channelId().equals(join.getChannelId())) return;
			Instant joinedAt = join.getCreatedAt() > 0
					? Instant.ofEpochMilli(join.getCreatedAt()) : Instant.now();
			states.put(key, new VoiceState(serverId, join.getChannelId(), join.getUserId(), joinedAt));
		} else if (event instanceof VoiceLeaveEvent leave) {
			Key key = new Key(serverId, leave.getUserId());
			VoiceState previous = states.get(key);
			if (previous != null && previous.channelId().equals(leave.getChannelId())) states.remove(key);
		} else if ("app.uninstalled".equals(event.getType())) {
			states.keySet().removeIf(key -> key.serverId().equals(serverId));
		} else if ("member.left".equals(event.getType()) || "member.banned".equals(event.getType())) {
			String userId = EntityJson.string(event.getData(), "user_id");
			if (userId != null) states.remove(new Key(serverId, userId));
		} else if ("channel.deleted".equals(event.getType())) {
			String channelId = EntityJson.string(EntityJson.object(event.getData(), "channel"), "id");
			if (channelId != null) states.values().removeIf(state ->
					state.serverId().equals(serverId) && state.channelId().equals(channelId));
		}
	}

	public synchronized VoiceState getState(String serverId, String userId) {
		return states.get(new Key(Objects.requireNonNull(serverId), Objects.requireNonNull(userId)));
	}

	public synchronized List<VoiceState> getChannelStates(String channelId) {
		Objects.requireNonNull(channelId, "channelId");
		return states.values().stream().filter(state -> channelId.equals(state.channelId())).toList();
	}

	public synchronized List<VoiceState> getServerStates(String serverId) {
		Objects.requireNonNull(serverId, "serverId");
		return states.values().stream().filter(state -> serverId.equals(state.serverId())).toList();
	}

	public synchronized void clear() {
		states.clear();
	}
}
