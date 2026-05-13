package com.ianscottbaker.ianbot.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface SlashCommand {
    SlashCommandData getCommandData();
    void execute(SlashCommandInteractionEvent event);
}
