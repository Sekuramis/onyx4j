package de.onyx4j.api.interactions.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * © Copyright 23.11.2026 - 16:52 – Urheberrechtshinweis
 * Alle Inhalte dieser Software, insbesondere der Quellcode, sind urheberrechtlich geschützt.
 * Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet,
 * bei @author Sekuramis | Jannik.
 * Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten.
 * Diese Software kann soweit möglich, als API von anderen Entwicklern verwendet werden.
 * Wer gegen das Urheberrecht verstößt (z.B. Quellcode unerlaubt kopiert), macht sich
 * gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und muss Schadensersatz leisten (§ 97 UrhG).
 */
public record SlashCommandData(
        String name,
        String description,
        String type,
        List<CommandOptionData> options
) {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9_-]{1,32}");

    public SlashCommandData {
        if (!"slash".equals(type) && !"user".equals(type) && !"message".equals(type))
            throw new IllegalArgumentException("Unsupported command type");
        options = List.copyOf(Objects.requireNonNull(options, "options"));
        if ("slash".equals(type)) {
            if (name == null || !NAME_PATTERN.matcher(name).matches())
                throw new IllegalArgumentException("Command name must match [a-z0-9_-]{1,32}");
            if (description == null || description.isBlank())
                throw new IllegalArgumentException("Command description must not be blank");
        } else {
            if (name == null || name.isBlank() || name.length() > 32 || name.chars().anyMatch(Character::isISOControl))
                throw new IllegalArgumentException("Invalid context command name");
            if (!options.isEmpty()) throw new IllegalArgumentException("Context commands do not accept options");
        }
    }

    public SlashCommandData(String name, String description) {
        this(name, description, "slash", List.of());
    }

    public SlashCommandData(String name, String description, String type) {
        this(name, description, type, List.of());
    }

    public SlashCommandData addOptions(CommandOptionData... additional) {
        Objects.requireNonNull(additional, "additional");

        List<CommandOptionData> combined = new ArrayList<>(options);

        for (CommandOptionData option : additional) {
            Objects.requireNonNull(option, "option");

            if (combined.stream().anyMatch(
                    existing -> existing.getName().equals(option.getName())
            )) {
                throw new IllegalArgumentException(
                        "Duplicate command option: " + option.getName()
                );
            }

            combined.add(option);
        }

        if (combined.size() > 25) {
            throw new IllegalArgumentException(
                    "At most 25 command options are supported"
            );
        }

        return new SlashCommandData(name, description, type, combined);
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();

        object.addProperty("name", name);
        if ("slash".equals(type)) object.addProperty("description", description);
        object.addProperty("type", type);

        if (!options.isEmpty()) {
            JsonArray array = new JsonArray();

            for (CommandOptionData option : options) {
                array.add(option.toJson());
            }

            object.add("options", array);
        }

        return object;
    }
}
