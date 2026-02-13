package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.model.CommandContext;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface ButtonHandler {
    boolean supports(String buttonId);
    void handle(ButtonInteractionEvent event, CommandContext ctx);
}
