package de.onyx4j.api.entities.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.onyx4j.api.OnyxClient;
import de.onyx4j.api.entities.channel.ServerChannel;
import de.onyx4j.api.entities.channel.TextChannel;
import de.onyx4j.api.entities.channel.VoiceChannel;
import de.onyx4j.api.entities.member.Member;
import de.onyx4j.api.entities.role.Permission;
import de.onyx4j.api.entities.role.Role;
import de.onyx4j.api.internal.EntityJson;
import de.onyx4j.api.requests.RestAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * © Copyright 23.11.2026 - 17:11 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class Server
{

	private final OnyxClient client;
	private final JsonObject data;

	public Server(OnyxClient client, JsonObject response)
	{
		this.client = Objects.requireNonNull(client, "client");
		JsonObject source = EntityJson.object(Objects.requireNonNull(response, "response"), "server");
		this.data = (source == null ? response : source).deepCopy();
		EntityJson.encode(getId());
	}

	public String getId()
	{
		return EntityJson.string(data, "id");
	}

	public String getName()
	{
		return EntityJson.string(data, "name");
	}

	public String getDescription()
	{
		return EntityJson.string(data, "description");
	}

	public String getOwnerId()
	{
		return EntityJson.string(data, "owner_id");
	}

	public String getIconUrl()
	{
		return EntityJson.string(data, "icon");
	}

	public JsonObject getRawData()
	{
		return data.deepCopy();
	}

	public RestAction<List<ServerChannel>> retrieveChannels()
	{
		return new RestAction<>(
				() -> client.getRestClient().get("/servers/" + encodedId() + "/channels").thenApply(result ->
				{
					List<ServerChannel> resultChannels = new ArrayList<>();
					for (JsonElement entry : EntityJson.requireArray(result))
					{
						JsonObject channel = EntityJson.requireObject(entry);
						String type = EntityJson.string(channel, "type");
						switch (type)
						{
						case "text" -> resultChannels.add(new TextChannel(client, channel));
						case "voice" -> resultChannels.add(new VoiceChannel(client, channel));
						default -> throw new IllegalStateException("Unknown Onyx channel type: " + type);
						}
					}
					return List.copyOf(resultChannels);
				}));
	}

	public RestAction<List<Role>> retrieveRoles()
	{
		return new RestAction<>(
				() -> client.getRestClient().get("/servers/" + encodedId() + "/roles").thenApply(result ->
				{
					List<Role> roles = new ArrayList<>();

					for (JsonElement entry : EntityJson.requireArray(result))
					{
						Role role = new Role(client, getId(), EntityJson.requireObject(entry));

						client.getEntityCache().putRole(role);
						roles.add(role);
					}

					return List.copyOf(roles);
				}));
	}

	public RestAction<List<Member>> retrieveMembers(int limit)
	{
		return retrieveMembers(limit, null);
	}

	public RestAction<List<Member>> retrieveMembers(int limit, String after)
	{
		if (limit < 1 || limit > 100)
		{
			throw new IllegalArgumentException("Member limit must be between 1 and 100");
		}

		String path = "/servers/" + encodedId() + "/members?limit=" + limit;

		if (after != null && !after.isBlank())
		{
			path += "&after=" + EntityJson.encode(after);
		}

		String target = path;

		return new RestAction<>(() -> client.getRestClient().get(target).thenApply(result ->
		{
			List<Member> members = new ArrayList<>();

			for (JsonElement entry : EntityJson.requireArray(result))
			{
				Member member = new Member(client, getId(), EntityJson.requireObject(entry));

				client.getEntityCache().putMember(member);
				members.add(member);
			}

			return List.copyOf(members);
		}));
	}

	public RestAction<Member> retrieveMemberById(String userId)
	{
		return new RestAction<>(
				() -> client.getRestClient().get("/servers/" + encodedId() + "/members/" + EntityJson.encode(userId))
						.thenApply(result -> new Member(client, getId(), EntityJson.requireObject(result))));
	}

	public RestAction<TextChannel> createTextChannel(String name)
	{
		return createChannel(name, "text").map(result -> new TextChannel(client, result));
	}

	public RestAction<VoiceChannel> createVoiceChannel(String name)
	{
		return createChannel(name, "voice").map(result -> new VoiceChannel(client, result));
	}

	public RestAction<Role> createRole(String name, Permission... permissions)
	{
		if (name == null || name.isBlank())
		{
			throw new IllegalArgumentException("Role name must not be blank");
		}
		int bits = Permission.toBits(Objects.requireNonNull(permissions));
		return new RestAction<>(() ->
		{
			JsonObject body = new JsonObject();
			body.addProperty("name", name);
			body.addProperty("permissions", bits);
			return client.getRestClient().post("/servers/" + encodedId() + "/roles", body).thenApply(result ->
			{
				JsonObject response = EntityJson.requireObject(result);
				JsonObject data = response.deepCopy();
				data.addProperty("name", name);
				data.addProperty("permissions", bits);
				return new Role(client, getId(), data);
			});
		});
	}

	public RestAction<Void> unban(String userId)
	{
		return new RestAction<>(
				() -> client.getRestClient().delete("/servers/" + encodedId() + "/bans/" + EntityJson.encode(userId))
						.thenApply(result -> null));
	}

	private RestAction<JsonObject> createChannel(String name, String type)
	{
		if (name == null || name.isBlank())
		{
			throw new IllegalArgumentException("Channel name must not be blank");
		}
		return new RestAction<>(() ->
		{
			JsonObject body = new JsonObject();
			body.addProperty("name", name);
			body.addProperty("type", type);
			return client.getRestClient().post("/servers/" + encodedId() + "/channels", body).thenApply(result ->
			{
				JsonObject response = EntityJson.requireObject(result);
				JsonObject channel = response.deepCopy();
				channel.addProperty("name", name);
				channel.addProperty("server_id", getId());
				channel.addProperty("type", type);
				return channel;
			});
		});
	}

	private String encodedId()
	{
		return EntityJson.encode(getId());
	}
}
