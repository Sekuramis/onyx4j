package de.onyx4j.api.entities.role;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * © Copyright 24.11.2026 - 01:06 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public record PermissionSet(int raw) {

	public static PermissionSet of(Permission... permissions) {
		return new PermissionSet(Permission.toBits(permissions));
	}

	public boolean has(Permission permission) {
		return Objects.requireNonNull(permission, "permission").isIn(raw);
	}

	public boolean hasAll(Permission... permissions) {
		for (Permission permission : permissions) {
			if (!has(permission)) return false;
		}
		return true;
	}

	public boolean hasAny(Permission... permissions) {
		for (Permission permission : permissions) {
			if (has(permission)) return true;
		}
		return false;
	}

	public Set<Permission> getExplicitPermissions() {
		EnumSet<Permission> result = EnumSet.noneOf(Permission.class);
		for (Permission permission : Permission.values()) {
			if ((raw & permission.getBit()) == permission.getBit()) result.add(permission);
		}
		return Collections.unmodifiableSet(result);
	}

	public Set<Permission> getEffectivePermissions() {
		EnumSet<Permission> result = EnumSet.noneOf(Permission.class);
		for (Permission permission : Permission.values()) {
			if (has(permission)) result.add(permission);
		}
		return Collections.unmodifiableSet(result);
	}
}
