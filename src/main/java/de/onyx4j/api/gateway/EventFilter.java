package de.onyx4j.api.gateway;

import de.onyx4j.api.events.GatewayEvent;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
/**
 * © Copyright 24.11.2026 - 00:33 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class EventFilter {

	private final Set<String> types;
	private final boolean all;

	private EventFilter(Set<String> types, boolean all) {
		this.types = Set.copyOf(types);
		this.all = all;
	}

	public static EventFilter all() {
		return new EventFilter(Set.of(), true);
	}

	public static EventFilter only(String... eventTypes) {
		Objects.requireNonNull(eventTypes, "eventTypes");
		if (eventTypes.length == 0) {
			throw new IllegalArgumentException("At least one event type is required");
		}
		Set<String> types = new LinkedHashSet<>();
		for (String type : eventTypes) {
			if (type == null || type.isBlank()) {
				throw new IllegalArgumentException("Event types must not be blank");
			}
			types.add(type);
		}
		if (types.contains("*")) {
			return all();
		}
		return new EventFilter(types, false);
	}

	public boolean allows(String eventType) {
		return eventType != null && (all || types.contains(eventType));
	}

	public boolean allows(GatewayEvent event) {
		return event != null && allows(event.getType());
	}

	public boolean allowsAll() {
		return all;
	}

	public Set<String> getAllowedTypes() {
		return types;
	}
}