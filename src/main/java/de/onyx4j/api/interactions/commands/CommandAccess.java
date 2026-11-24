package de.onyx4j.api.interactions.commands;

import com.google.gson.JsonElement;
import de.onyx4j.api.entities.member.Member;
import de.onyx4j.api.entities.role.Permission;
import de.onyx4j.api.entities.role.PermissionSet;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.interactions.commands.annotation.RequirePermissions;
import de.onyx4j.api.interactions.commands.annotation.RequireRoles;
import de.onyx4j.api.interactions.commands.annotation.ServerOnly;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * © Copyright 24.11.2026 - 01:06 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class CommandAccess {

	private final boolean serverOnly;
	private final List<Permission> permissions;
	private final List<String> roles;
	private final boolean anyPermission;
	private final boolean anyRole;

	private CommandAccess(boolean serverOnly, List<Permission> permissions, List<String> roles,
			boolean anyPermission, boolean anyRole) {
		this.serverOnly = serverOnly;
		this.permissions = List.copyOf(permissions);
		this.roles = List.copyOf(roles);
		this.anyPermission = anyPermission;
		this.anyRole = anyRole;
	}

	public static CommandAccess from(Method method) {
		RequirePermissions permissions = method.getAnnotation(RequirePermissions.class);
		RequireRoles roles = method.getAnnotation(RequireRoles.class);
		List<Permission> requiredPermissions = permissions == null ? List.of() : List.of(permissions.value());
		List<String> requiredRoles = roles == null ? List.of() : List.of(roles.value());

		if (permissions != null && requiredPermissions.isEmpty())
			throw new IllegalArgumentException("Empty permission requirement: " + method);
		if (roles != null && (requiredRoles.isEmpty() || requiredRoles.stream().anyMatch(String::isBlank)))
			throw new IllegalArgumentException("Invalid role requirement: " + method);

		return new CommandAccess(method.isAnnotationPresent(ServerOnly.class) || permissions != null || roles != null,
				requiredPermissions, requiredRoles, permissions != null && permissions.any(), roles != null && roles.any());
	}

	public boolean isProtected() {
		return serverOnly;
	}

	public boolean requiresMember() {
		return !permissions.isEmpty() || !roles.isEmpty();
	}

	public boolean allows(Member member) {
		if (!requiresMember()) return true;
		if (member == null) return false;

		if (!permissions.isEmpty() && !isVerifiedOwner(member)) {
			JsonElement value = member.getRawData().get("permissions");
			if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return false;
			int raw;
			try {
				raw = value.getAsBigDecimal().intValueExact();
			} catch (ArithmeticException | NumberFormatException exception) {
				return false;
			}
			if (raw < 0) return false;
			PermissionSet set = new PermissionSet(raw);
			Permission[] requested = permissions.toArray(Permission[]::new);
			if (!(anyPermission ? set.hasAny(requested) : set.hasAll(requested))) return false;
		}

		if (!roles.isEmpty()) {
			JsonElement assigned = member.getRawData().get("roles");
			if (assigned == null || !assigned.isJsonArray()) return false;
			for (JsonElement role : assigned.getAsJsonArray()) {
				if (!role.isJsonPrimitive() || !role.getAsJsonPrimitive().isString()) return false;
			}
			List<String> assignedRoles = member.getRoleIds();
			if (!(anyRole ? roles.stream().anyMatch(assignedRoles::contains) : assignedRoles.containsAll(roles)))
				return false;
		}
		return true;
	}

	private static boolean isVerifiedOwner(Member member) {
		JsonElement owner = member.getRawData().get("owner");
		return owner != null && owner.isJsonPrimitive() && owner.getAsJsonPrimitive().isBoolean()
				&& owner.getAsBoolean();
	}

	public CompletableFuture<Boolean> authorize(SlashCommandInteractionEvent event) {
		if (!serverOnly) return CompletableFuture.completedFuture(true);
		String serverId = event.getServerId();
		if (serverId == null || serverId.isBlank()) return CompletableFuture.completedFuture(false);
		if (!requiresMember()) return CompletableFuture.completedFuture(true);
		String userId = event.getUser().getId();
		if (event.getClient() == null || userId == null || userId.isBlank())
			return CompletableFuture.completedFuture(false);
		return event.getClient().retrieveMemberById(serverId, userId).queue()
				.orTimeout(1800, TimeUnit.MILLISECONDS)
				.thenApply(member -> serverId.equals(member.getServerId()) && userId.equals(member.getId()) && allows(member));
	}
}
