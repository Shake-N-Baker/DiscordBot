package com.ianscottbaker.ianbot.command;

import com.ianscottbaker.ianbot.model.IBUser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

public class Blackjack {
    // game state representation:
    // Full vs Short
    // 2=2
    // 3=3
    // 4=4
    // 5=5
    // 6=6
    // 7=7
    // 8=8
    // 9=9
    // 10=1
    // Jack=J
    // Queen=Q
    // King=K
    // Ace=A
    // player hand, dealer hand
    // i.e. game start, player gets 4 and J, dealer
    public static void execute(@NotNull SlashCommandInteractionEvent event,
                               @NotNull IBUser ibUser,
                               @NotNull MongoCollection<Document> userCollection,
                               Document updateQuery,
                               UpdateOptions updateOptions
    ) {
        OptionMapping interactionMapping = event.getOption("interaction");
        if (interactionMapping == null) {
            event.reply("Please provide a valid blackjack command").setEphemeral(true).queue();
            return;
        }

        String interaction = interactionMapping.getAsString();
        MessageChannelUnion messageChannelUnion = event.getChannel();
        Button addButton = Button.success("addId", "add");
        Button removeButton = Button.danger("removeId", "remove");
        MessageCreateData messageCreateData = new MessageCreateBuilder()
                .addContent(String.format("interaction: %s", interaction))
                .setActionRow(addButton, removeButton)
                .build();
        event.reply(messageCreateData).setEphemeral(true).queue();
    }
}
