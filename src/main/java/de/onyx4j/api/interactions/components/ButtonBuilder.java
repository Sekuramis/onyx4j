package de.onyx4j.api.interactions.components;

import com.google.gson.JsonObject;

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
public final class ButtonBuilder {

	private final String target;
	private final String label;
	private final ButtonStyle style;
	private String emoji;
	private boolean disabled;

	private ButtonBuilder(String target, String label, ButtonStyle style) {
		this.target = target;
		this.label = label;
		this.style = Objects.requireNonNull(style, "style");
	}

	public static ButtonBuilder of(ButtonStyle style, String customId, String label) {
		if (style == ButtonStyle.LINK)
			throw new IllegalArgumentException("Use ButtonBuilder.link for link buttons");
		return new ButtonBuilder(customId, label, style);
	}

	public static ButtonBuilder link(String url, String label) {
		return new ButtonBuilder(url, label, ButtonStyle.LINK);
	}

	public ButtonBuilder setEmoji(String emoji) {
		this.emoji = emoji;
		return this;
	}

	public ButtonBuilder setDisabled(boolean disabled) {
		this.disabled = disabled;
		return this;
	}

	public JsonObject build() {
		JsonObject button = style == ButtonStyle.LINK ? Components.link(target, label)
				: Components.button(target, label, style.getValue());
		if (emoji != null) button = Components.emoji(button, emoji);
		if (disabled) button = Components.disabled(button);
		return button;
	}
}
