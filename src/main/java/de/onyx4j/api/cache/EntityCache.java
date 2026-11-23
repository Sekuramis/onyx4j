package de.onyx4j.api.cache;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.member.Member;
import de.onyx4j.api.entities.role.Role;
import de.onyx4j.api.events.GatewayEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
/**
 * © Copyright 24.11.2026 - 00:34 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class EntityCache {

	private final OnyxClient client;
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, JsonObject>> members = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, ConcurrentHashMap<String, JsonObject>> roles = new ConcurrentHashMap<>();

	public EntityCache(OnyxClient client) {
		this.client = Objects.requireNonNull(client, "client");
	}

	public void putMember(Member member) {
		Objects.requireNonNull(member, "member");
		String serverId = requireId(member.getServerId());
		String userId = requireId(member.getId());
		members.computeIfAbsent(serverId, ignored -> new ConcurrentHashMap<>())
				.put(userId, member.getRawData());
	}

	public void putRole(Role role) {
		Objects.requireNonNull(role, "role");
		String serverId = requireId(role.getServerId());
		String roleId = requireId(role.getId());
		roles.computeIfAbsent(serverId, ignored -> new ConcurrentHashMap<>())
				.put(roleId, role.getRawData());
	}

	public Member getMember(String serverId, String userId) {
		JsonObject data = lookup(members, serverId, userId);
		return data == null ? null : new Member(client, serverId, data.deepCopy());
	}

	public Role getRole(String serverId, String roleId) {
		JsonObject data = lookup(roles, serverId, roleId);
		return data == null ? null : new Role(client, serverId, data.deepCopy());
	}

	public List<Member> getCachedMembers(String serverId) {
		ConcurrentHashMap<String, JsonObject> entries = members.get(requireId(serverId));
		if (entries == null) {
			return List.of();
		}
		List<Member> snapshot = new ArrayList<>();
		entries.forEach((id, data) -> snapshot.add(new Member(client, serverId, data.deepCopy())));
		return List.copyOf(snapshot);
	}

	public List<Role> getCachedRoles(String serverId) {
		ConcurrentHashMap<String, JsonObject> entries = roles.get(requireId(serverId));
		if (entries == null) {
			return List.of();
		}
		List<Role> snapshot = new ArrayList<>();
		entries.forEach((id, data) -> snapshot.add(new Role(client, serverId, data.deepCopy())));
		return List.copyOf(snapshot);
	}

	public void invalidateMember(String serverId, String userId) {
		remove(members, requireId(serverId), requireId(userId));
	}

	public void invalidateRole(String serverId, String roleId) {
		remove(roles, requireId(serverId), requireId(roleId));
		members.remove(serverId);
	}

	public void invalidateServer(String serverId) {
		String id = requireId(serverId);
		members.remove(id);
		roles.remove(id);
	}

	public void clear() {
		members.clear();
		roles.clear();
	}

	public void apply(GatewayEvent event) {
		Objects.requireNonNull(event, "event");
		String serverId = event.getServerId();
		String type = event.getType();
		if (serverId == null || serverId.isBlank() || type == null) {
			return;
		}
		JsonObject data = event.getData();
		switch (type) {
		case "member.joined", "member.left", "member.banned" -> {
			String userId = "member.joined".equals(type)
					? string(object(data, "user"), "id")
					: string(data, "user_id");
			if (userId != null) {
				remove(members, serverId, userId);
			}
		}
		case "member.roles_updated" -> {
			String userId = string(data, "user_id");
			JsonArray roleIds = array(data, "role_ids");
			if (userId != null && roleIds != null) {
				updateMember(serverId, userId, "roles", roleIds.deepCopy());
			}
		}
		case "member.timeout" -> {
			String userId = string(data, "user_id");
			JsonElement until = data.get("until");
			if (userId != null && until != null && !until.isJsonNull()) {
				updateMember(serverId, userId, "timeout_until", until.deepCopy());
			}
		}
		case "role.created", "role.updated" -> {
			JsonObject role = object(data, "role");
			String roleId = string(role, "id");
			if (roleId != null) {
				assert role != null;
				roles.computeIfAbsent(serverId, ignored -> new ConcurrentHashMap<>())
						.put(roleId, role.deepCopy());
				if ("role.updated".equals(type)) {
					members.remove(serverId);
				}
			}
		}
		case "role.deleted" -> {
			JsonObject role = object(data, "role");
			String roleId = string(role, "id");
			if (roleId != null) {
				invalidateRole(serverId, roleId);
			}
		}
		case "app.uninstalled" -> invalidateServer(serverId);
		default -> {
		}
		}
	}

	private void updateMember(String serverId, String userId, String key, JsonElement value) {
		ConcurrentHashMap<String, JsonObject> entries = members.get(serverId);
		if (entries == null) {
			return;
		}
		entries.computeIfPresent(userId, (id, previous) -> {
			if ("roles".equals(key) && previous.has("permissions")) {
				return null;
			}
			JsonObject updated = previous.deepCopy();
			updated.add(key, value);
			return updated;
		});
	}

	private static JsonObject lookup(
			ConcurrentHashMap<String, ConcurrentHashMap<String, JsonObject>> cache,
			String serverId,
			String entityId
	) {
		ConcurrentHashMap<String, JsonObject> entries = cache.get(requireId(serverId));
		return entries == null ? null : entries.get(requireId(entityId));
	}

	private static void remove(
			ConcurrentHashMap<String, ConcurrentHashMap<String, JsonObject>> cache,
			String serverId,
			String entityId
	) {
		ConcurrentHashMap<String, JsonObject> entries = cache.get(serverId);
		if (entries != null) {
			entries.remove(entityId);
		}
	}

	private static String requireId(String id) {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("Entity ID is required");
		}
		return id;
	}

	private static String string(JsonObject object, String key) {
		JsonElement value = object == null ? null : object.get(key);
		return value == null || value.isJsonNull() ? null : value.getAsString();
	}

	private static JsonObject object(JsonObject object, String key) {
		JsonElement value = object == null ? null : object.get(key);
		return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
	}

	private static JsonArray array(JsonObject object, String key) {
		JsonElement value = object == null ? null : object.get(key);
		return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
	}
}