package de.onyx4j.api.entities.role;

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
public enum Permission {

    ADMINISTRATOR(1),
    MANAGE_SERVER(2),
    MANAGE_CHANNELS(4),
    MANAGE_ROLES(8),
    KICK(16),
    BAN(32),
    TIMEOUT(64),
    MANAGE_MESSAGES(128),
    SEND_MESSAGES(256),
    ATTACH_FILES(512),
    CONNECT(1024),
    SPEAK(2048),
    VIDEO(4096),
    CREATE_INVITE(8192),
    MENTION_EVERYONE(16384),
    VIEW_AUDIT(32768),
    MANAGE_WEBHOOKS(65536),
    ADD_REACTIONS(131072);

    private final int bit;

    Permission(int bit) {
        this.bit = bit;
    }

    public int getBit() {
        return bit;
    }

    public static int toBits(Permission... permissions) {
        int result = 0;
        for (Permission permission : permissions) {
            if (permission == null) {
                throw new IllegalArgumentException("Permissions must not contain null");
            }
            result |= permission.bit;
        }
        return result;
    }

    public boolean isIn(int bits) {
        return (bits & ADMINISTRATOR.bit) != 0 || (bits & bit) == bit;
    }
}
