package de.onyx4j.api.entities.role;

import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;

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
public final class Role {

    private final OnyxClient client;
    private final String serverId;
    private final JsonObject data;

    public Role(OnyxClient client, String serverId, JsonObject data) {
        this.client = Objects.requireNonNull(client, "client");
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.data = Objects.requireNonNull(data, "data").deepCopy();
    }

    public String getId() {
        return EntityJson.string(data, "id");
    }

    public String getServerId() {
        return serverId;
    }

    public String getName() {
        return EntityJson.string(data, "name");
    }

    public String getColor() {
        return EntityJson.string(data, "color");
    }

    public int getPosition() {
        return EntityJson.number(data, "position");
    }

    public int getPermissionsRaw() {
        return EntityJson.number(data, "permissions");
    }

    public boolean isDefault() {
        return EntityJson.bool(data, "is_default");
    }

    public boolean isHoisted() {
        return EntityJson.bool(data, "hoist");
    }

    public boolean isManaged() {
        return EntityJson.string(data, "managed_app") != null;
    }

    public boolean hasPermission(Permission permission) {
        return getPermissions().has(permission);
    }

    public PermissionSet getPermissions() {
        return new PermissionSet(getPermissionsRaw());
    }

    public boolean hasPermissions(Permission... permissions) {
        return getPermissions().hasAll(permissions);
    }

    public boolean hasAnyPermission(Permission... permissions) {
        return getPermissions().hasAny(permissions);
    }

    public RestAction<Void> editName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name must not be blank");
        }
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        return edit(body);
    }

    public RestAction<Void> editColor(String color) {
        JsonObject body = new JsonObject();
        if (color == null) {
            body.add("color", com.google.gson.JsonNull.INSTANCE);
        } else {
            body.addProperty("color", color);
        }
        return edit(body);
    }

    public RestAction<Void> editPermissions(Permission... permissions) {
        JsonObject body = new JsonObject();
        body.addProperty("permissions", Permission.toBits(permissions));
        return edit(body);
    }

    public RestAction<Void> edit(JsonObject changes) {
        JsonObject payload = Objects.requireNonNull(changes).deepCopy();
        return new RestAction<>(() ->
                client.getRestClient()
                        .patch("/roles/" + EntityJson.encode(getId()), payload)
                        .thenApply(response -> null)
        );
    }

    public RestAction<Void> delete() {
        return new RestAction<>(() ->
                client.getRestClient()
                        .delete("/roles/" + EntityJson.encode(getId()))
                        .thenApply(response -> null)
        );
    }

    public JsonObject getRawData() {
        return data.deepCopy();
    }
}
