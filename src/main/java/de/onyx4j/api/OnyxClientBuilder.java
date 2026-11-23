package de.onyx4j.api;

import de.onyx4j.api.events.OnyxListenerAdapter;
import de.onyx4j.api.interactions.commands.SlashCommandData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
/**
 * © Copyright 23.11.2026 - 16:51 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class OnyxClientBuilder {

    private final String token;
    private String baseUrl = "https://onyx-clips.de";

    private final java.util.List<SlashCommandData> registeredCommands =
            new java.util.ArrayList<>();
    private boolean autoSyncCommands = true;

    private final List<OnyxListenerAdapter> listeners =
            new ArrayList<>();

    private OnyxClientBuilder(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "An Onyx app token is required"
            );
        }

        this.token = token;
    }

    public static OnyxClientBuilder createDefault(String token) {
        return new OnyxClientBuilder(token);
    }

    public OnyxClientBuilder setBaseUrl(String baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl);
        return this;
    }

    public OnyxClientBuilder addEventListeners(
            OnyxListenerAdapter... listeners
    ) {
        this.listeners.addAll(Arrays.asList(listeners));
        return this;
    }

    public OnyxClient build() {
        OnyxClient client = new OnyxClient(
                token,
                baseUrl,
                List.copyOf(listeners)
        );

        client.getCommandRegistry()
                .addCommands(registeredCommands.toArray(SlashCommandData[]::new))
                .setAutoSync(autoSyncCommands);

        client.connect();
        return client;
    }


    public OnyxClientBuilder addCommands(SlashCommandData... commands) {
        registeredCommands.addAll(java.util.List.of(commands));
        return this;
    }

    public OnyxClientBuilder setAutoSyncCommands(boolean enabled) {
        autoSyncCommands = enabled;
        return this;
    }

}