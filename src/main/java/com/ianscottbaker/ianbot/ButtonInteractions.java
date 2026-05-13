package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.command.ButtonHandler;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ButtonInteractions extends ListenerAdapter {
    private final Map<String, ButtonHandler> handlersByPrefix;

    public ButtonInteractions(List<ButtonHandler> handlers) {
        this.handlersByPrefix = handlers.stream()
                .collect(Collectors.toMap(ButtonHandler::getPrefix, Function.identity()));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        if (buttonId == null) {
            event.reply("buttonId is null").queue();
            return;
        }

        int delimiterIndex = buttonId.indexOf(ButtonHandler.DELIMITER);
        String prefix = delimiterIndex < 0 ? buttonId : buttonId.substring(0, delimiterIndex);
        String payload = delimiterIndex < 0 ? "" : buttonId.substring(delimiterIndex + ButtonHandler.DELIMITER.length());

        ButtonHandler handler = handlersByPrefix.get(prefix);
        if (handler == null) {
            event.reply(String.format("uncaught buttonId %s", buttonId)).queue();
            return;
        }
        handler.handle(event, payload);
    }
}
