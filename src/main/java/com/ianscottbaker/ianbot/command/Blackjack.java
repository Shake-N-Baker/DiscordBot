package com.ianscottbaker.ianbot.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.stereotype.Service;

@Service
public class Blackjack implements SlashCommand, ButtonHandler {
    public static final String NAME = "blackjack";
    public static final String BUTTON_PREFIX = "blackjack";
    public static final String ACTION_HIT = "hit";
    public static final String ACTION_STAY = "stay";

    @Override
    public SlashCommandData getCommandData() {
        OptionData option = new OptionData(OptionType.STRING, "interaction", "blackjack interaction", true)
                .addChoice("new", "new")
                .addChoice("stats", "stats")
                .addChoice("hit", "hit")
                .addChoice("stand", "stand")
                .addChoice("double", "double")
                .addChoice("split", "split");
        return Commands.slash(NAME, "play blackjack").addOptions(option);
    }

    @Override
    public String getPrefix() {
        return BUTTON_PREFIX;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping interactionMapping = event.getOption("interaction");
        if (interactionMapping == null) {
            event.reply("Please provide a valid blackjack command").setEphemeral(true).queue();
            return;
        }

        String interaction = interactionMapping.getAsString();
        Button hitButton = Button.success(buttonId(ACTION_HIT), "hit");
        Button stayButton = Button.danger(buttonId(ACTION_STAY), "stay");
        MessageCreateData messageCreateData = new MessageCreateBuilder()
                .addContent(String.format("interaction: %s", interaction))
                .setActionRow(hitButton, stayButton)
                .build();
        event.reply(messageCreateData).setEphemeral(true).queue();
    }

    @Override
    public void handle(ButtonInteractionEvent event, String payload) {
        String action = payload.isEmpty() ? "" : payload.split(DELIMITER, 2)[0];
        switch (action) {
            case ACTION_HIT -> event.reply("hit pressed").queue();
            case ACTION_STAY -> event.reply("stay pressed").queue();
            default -> event.reply(String.format("uncaught blackjack payload %s", payload)).queue();
        }
    }

    private static String buttonId(String action) {
        return BUTTON_PREFIX + DELIMITER + action;
    }
}
