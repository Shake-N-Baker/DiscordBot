package com.ianscottbaker.ianbot.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Service;

@Service
public class Say implements SlashCommand {
    public static final String NAME = "say";

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash(NAME, "say something")
                .addOptions(new OptionData(OptionType.STRING, "say", "what to say", true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping sayMapping = event.getOption("say");
        if (sayMapping == null) {
            event.reply("Please provide a valid say command").setEphemeral(true).queue();
            return;
        }
        event.reply(sayMapping.getAsString()).setEphemeral(false).queue();
    }
}
