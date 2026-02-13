package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.model.CommandContext;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface CommandHandler {
    String getCommandName();
    void handle(SlashCommandInteractionEvent event, CommandContext ctx);
}
