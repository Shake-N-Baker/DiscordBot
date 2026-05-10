package com.ianscottbaker.ianbot.command;

import com.ianscottbaker.ianbot.model.IBUser;
import com.ianscottbaker.ianbot.repository.IBUserRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class Blackjack {
    public static final String HIT_BUTTON_ID = "blackjack-hit";
    public static final String STAY_BUTTON_ID = "blackjack-stay";

    public void execute(@NotNull SlashCommandInteractionEvent event,
                        @NotNull IBUser ibUser,
                        @NotNull IBUserRepository userRepository) {
        OptionMapping interactionMapping = event.getOption("interaction");
        if (interactionMapping == null) {
            event.reply("Please provide a valid blackjack command").setEphemeral(true).queue();
            return;
        }

        String interaction = interactionMapping.getAsString();
        Button hitButton = Button.success(HIT_BUTTON_ID, "hit");
        Button stayButton = Button.danger(STAY_BUTTON_ID, "stay");
        MessageCreateData messageCreateData = new MessageCreateBuilder()
                .addContent(String.format("interaction: %s", interaction))
                .setActionRow(hitButton, stayButton)
                .build();
        event.reply(messageCreateData).setEphemeral(true).queue();
    }
}
