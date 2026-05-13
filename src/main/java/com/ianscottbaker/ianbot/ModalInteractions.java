package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.command.ModalHandler;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ModalInteractions extends ListenerAdapter {
    private final Map<String, ModalHandler> handlersByPrefix;

    public ModalInteractions(List<ModalHandler> handlers) {
        this.handlersByPrefix = handlers.stream()
                .collect(Collectors.toMap(ModalHandler::getPrefix, Function.identity()));
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        int delimiterIndex = modalId.indexOf(ModalHandler.DELIMITER);
        String prefix = delimiterIndex < 0 ? modalId : modalId.substring(0, delimiterIndex);
        String payload = delimiterIndex < 0 ? "" : modalId.substring(delimiterIndex + ModalHandler.DELIMITER.length());

        ModalHandler handler = handlersByPrefix.get(prefix);
        if (handler == null) {
            event.reply(String.format("uncaught modalId %s", modalId)).queue();
            return;
        }
        handler.handle(event, payload);
    }
}
