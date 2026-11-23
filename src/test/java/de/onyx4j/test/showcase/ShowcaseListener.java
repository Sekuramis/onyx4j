package de.onyx4j.test.showcase;

import de.onyx4j.api.events.OnyxListenerAdapter;
import de.onyx4j.api.events.interaction.AutocompleteInteractionEvent;
import de.onyx4j.api.events.interaction.ComponentInteractionEvent;
import de.onyx4j.api.events.interaction.ModalInteractionEvent;
import de.onyx4j.api.events.interaction.SlashCommandInteractionEvent;
import de.onyx4j.api.events.message.MessageReceivedEvent;
import de.onyx4j.api.interactions.commands.CommandOptionChoice;
import de.onyx4j.api.interactions.components.Components;
import de.onyx4j.api.interactions.modals.ModalForm;

/**
 * © Copyright 23.11.2026 - 23:14 – Urheberrechtshinweis Alle Inhalte dieser Software, insbesondere der Quellcode, sind
 * urheberrechtlich geschützt. Das Urheberrecht liegt, soweit nicht ausdrücklich anders gekennzeichnet, bei @author
 * Sekuramis | Jannik. Bitte fragen Sie mich, falls Sie die Inhalte dieser Software verwenden möchten. Diese Software
 * kann soweit möglich, als API von anderen Entwicklern verwendet werden. Wer gegen das Urheberrecht verstößt (z.B.
 * Quellcode unerlaubt kopiert), macht sich gem. §§ 106 ff. UrhG strafbar und wird zudem kostenpflichtig abgemahnt und
 * muss Schadensersatz leisten (§ 97 UrhG).
 */
public final class ShowcaseListener extends OnyxListenerAdapter
{

	@Override
	public void onMessageReceived(MessageReceivedEvent event)
	{
		if (event.getMessage().isAuthorBot() || event.getMessage().isContentHidden())
		{
			return;
		}
		if (!"!demo".equalsIgnoreCase(event.getMessage().getContentRaw()))
		{
			return;
		}
		event.getChannel().sendMessage(Components.message("Onyx4J 0.5.0 - Interactive Showcase",
				Components.row(Components.primary("showcase:ping", "Ping"),
						Components.secondary("showcase:feedback", "Feedback")), Components.row(
						Components.select("showcase:language", "Choose a language", Components.option("Java", "java"),
								Components.option("Kotlin", "kotlin"))))).queue();
	}

	@Override
	public void onComponentInteraction(ComponentInteractionEvent event)
	{
		String customId = event.getCustomId();
		if ("showcase:ping".equals(customId))
		{
			event.replyEphemeral("Pong from Onyx4J 0.5.0!").queue();
		} else if ("showcase:language".equals(customId))
		{
			String value = event.getValues().isEmpty() ? "none" : event.getValues().get(0);
			event.replyEphemeral("Your selection: " + value).queue();
		} else if ("showcase:feedback".equals(customId))
		{
			event.openModal(ModalForm.create("showcase:feedback:submit", "Onyx4J Feedback")
					.paragraphField("feedback", "Your feedback", true).submitLabel("Send")).queue();
		}
	}

	@Override
	public void onModalInteraction(ModalInteractionEvent event)
	{
		if ("showcase:feedback:submit".equals(event.getCustomId()))
		{
			String feedback = event.getValue("feedback");
			if (feedback == null || feedback.isBlank())
			{
				event.replyEphemeral("Please enter your feedback.").queue();
			} else
			{
				event.replyEphemeral("Thank you! Feedback received for this test.").queue();
			}
		}
	}

	@Override
	public void onAutocompleteInteraction(AutocompleteInteractionEvent event)
	{
		if (!"language".equals(event.getCommandName()) || !"name".equals(event.getFocusedOption()))
		{
			event.replyChoices().queue();
			return;
		}

		String query = event.getFocusedValue().toLowerCase(java.util.Locale.ROOT);

		java.util.List<CommandOptionChoice> choices = java.util.stream.Stream.of("Java", "Kotlin", "Scala")
				.filter(language -> language.toLowerCase(java.util.Locale.ROOT).startsWith(query))
				.map(language -> CommandOptionChoice.of(language, language.toLowerCase(java.util.Locale.ROOT)))
				.toList();

		event.replyChoices(choices).queue();
	}

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
	{
		if ("language".equals(event.getName()))
			event.reply("Selected language: " + event.getOption("name")).queue();
	}

	@Override
	public void onError(Throwable error)
	{
		error.printStackTrace();
	}
}