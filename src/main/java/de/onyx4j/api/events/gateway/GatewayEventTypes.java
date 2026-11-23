package de.onyx4j.api.events.gateway;

/**
 * © Copyright 24.11.2026 - 00:53 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class GatewayEventTypes {

	public static final String ALL = "*";
	public static final String MESSAGE_CREATED = "message.created";
	public static final String MESSAGE_UPDATED = "message.updated";
	public static final String MESSAGE_DELETED = "message.deleted";
	public static final String REACTION_ADDED = "reaction.added";
	public static final String REACTION_REMOVED = "reaction.removed";
	public static final String MEMBER_JOINED = "member.joined";
	public static final String MEMBER_LEFT = "member.left";
	public static final String MEMBER_ROLES_UPDATED = "member.roles_updated";
	public static final String MEMBER_BANNED = "member.banned";
	public static final String MEMBER_UNBANNED = "member.unbanned";
	public static final String MEMBER_TIMEOUT = "member.timeout";
	public static final String VOICE_JOINED = "voice.joined";
	public static final String VOICE_LEFT = "voice.left";
	public static final String AUTOMOD_BLOCKED = "automod.blocked";
	public static final String CHANNEL_CREATED = "channel.created";
	public static final String CHANNEL_UPDATED = "channel.updated";
	public static final String CHANNEL_DELETED = "channel.deleted";
	public static final String ROLE_CREATED = "role.created";
	public static final String ROLE_UPDATED = "role.updated";
	public static final String ROLE_DELETED = "role.deleted";
	public static final String APP_INSTALLED = "app.installed";
	public static final String APP_UNINSTALLED = "app.uninstalled";
	public static final String PING = "ping";

	private GatewayEventTypes() {
		throw new UnsupportedOperationException("Utility class");
	}
}