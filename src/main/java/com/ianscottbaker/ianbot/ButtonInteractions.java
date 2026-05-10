package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.command.Blackjack;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class ButtonInteractions extends ListenerAdapter {

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        if (buttonId == null) {
            event.reply("buttonId is null").queue();
            return;
        }

        if (buttonId.equals(Blackjack.HIT_BUTTON_ID)) {
            event.reply("hit pressed").queue();
        } else if (buttonId.equals(Blackjack.STAY_BUTTON_ID)) {
            event.reply("stay pressed").queue();
        } else {
            event.reply(String.format("uncaught buttonId %s", buttonId)).queue();
        }
    }
}
