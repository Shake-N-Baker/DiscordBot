package com.ianscottbaker.ianbot.command;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface ButtonHandler {
    String DELIMITER = ":";

    String getPrefix();
    void handle(ButtonInteractionEvent event, String payload);
}
