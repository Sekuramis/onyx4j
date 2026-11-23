package de.onyx4j.api.entities.member;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.User;
import de.onyx4j.api.entities.role.Permission;
import de.onyx4j.api.entities.role.Role;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;

import java.util.List;
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
public final class Member {

    private final OnyxClient client;
    private final String serverId;
    private final JsonObject data;

    public Member(OnyxClient client, String serverId, JsonObject data) {
        this.client = Objects.requireNonNull(client, "client");
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.data = Objects.requireNonNull(data, "data").deepCopy();
    }

    public String getId() {
        return EntityJson.string(EntityJson.object(data, "user"), "id");
    }

    public String getServerId() {
        return serverId;
    }

    public User getUser() {
        JsonObject json = EntityJson.object(data, "user");
        if (json == null) {
            return null;
        }
        return new User(
                EntityJson.string(json, "id"),
                EntityJson.string(json, "username"),
                EntityJson.string(json, "display_name")
        );
    }

    public String getNickname() {
        return EntityJson.string(data, "nickname");
    }

    public List<String> getRoleIds() {
        return EntityJson.strings(data, "roles");
    }

    public long getJoinedAt() {
        return EntityJson.longNumber(data, "joined_at");
    }

    public long getTimeoutUntil() {
        return EntityJson.longNumber(data, "timeout_until");
    }

    public boolean isOwner() {
        return EntityJson.bool(data, "owner");
    }

    public int getPermissionsRaw() {
        return EntityJson.number(data, "permissions");
    }

    public boolean hasPermission(Permission permission) {
        return Objects.requireNonNull(permission).isIn(getPermissionsRaw());
    }

    public RestAction<Void> addRole(Role role) {
        requireServerRole(role);
        return addRoleById(role.getId());
    }

    public RestAction<Void> addRoleById(String roleId) {
        return new RestAction<>(() ->
                client.getRestClient()
                        .put(memberPath() + "/roles/" + EntityJson.encode(roleId), new JsonObject())
                        .thenApply(result -> null)
        );
    }

    public RestAction<Void> removeRole(Role role) {
        requireServerRole(role);
        return removeRoleById(role.getId());
    }

    public RestAction<Void> removeRoleById(String roleId) {
        return new RestAction<>(() ->
                client.getRestClient()
                        .delete(memberPath() + "/roles/" + EntityJson.encode(roleId))
                        .thenApply(result -> null)
        );
    }

    public RestAction<Void> setRoles(List<String> roleIds) {
        Objects.requireNonNull(roleIds, "roleIds");
        JsonArray ids = new JsonArray();
        for (String id : roleIds) {
            EntityJson.encode(id);
            ids.add(id);
        }
        JsonObject body = new JsonObject();
        body.add("role_ids", ids);
        return new RestAction<>(() ->
                client.getRestClient()
                        .put(memberPath() + "/roles", body)
                        .thenApply(result -> null)
        );
    }

    public RestAction<Void> timeout(int minutes, String reason) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Timeout minutes must be nonnegative");
        }
        JsonObject body = new JsonObject();
        body.addProperty("minutes", minutes);
        if (reason != null) {
            body.addProperty("reason", reason);
        }
        return new RestAction<>(() ->
                client.getRestClient()
                        .post(memberPath() + "/timeout", body)
                        .thenApply(result -> null)
        );
    }

    public RestAction<Void> removeTimeout() {
        return timeout(0, null);
    }

    public RestAction<Void> kick(String reason) {
        JsonObject body = new JsonObject();
        if (reason != null) {
            body.addProperty("reason", reason);
        }
        return new RestAction<>(() ->
                client.getRestClient()
                        .post(memberPath() + "/kick", body)
                        .thenApply(result -> null)
        );
    }

    public RestAction<Void> ban(String reason, boolean deleteMessages) {
        JsonObject body = new JsonObject();
        if (reason != null) {
            body.addProperty("reason", reason);
        }
        if (deleteMessages) {
            body.addProperty("delete_messages", true);
        }
        return new RestAction<>(() ->
                client.getRestClient()
                        .post(memberPath() + "/ban", body)
                        .thenApply(result -> null)
        );
    }

    public JsonObject getRawData() {
        return data.deepCopy();
    }

    private String memberPath() {
        return "/servers/" + EntityJson.encode(serverId) + "/members/" + EntityJson.encode(getId());
    }

    private void requireServerRole(Role role) {
        if (!serverId.equals(Objects.requireNonNull(role).getServerId())) {
            throw new IllegalArgumentException("Role belongs to a different server");
        }
    }
}
