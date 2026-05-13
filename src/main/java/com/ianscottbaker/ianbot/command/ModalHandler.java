package com.ianscottbaker.ianbot.command;

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public interface ModalHandler {
    String DELIMITER = ":";

    String getPrefix();
    void handle(ModalInteractionEvent event, String payload);
}
