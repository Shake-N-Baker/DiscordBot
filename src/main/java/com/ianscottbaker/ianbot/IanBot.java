package com.ianscottbaker.ianbot;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.ianscottbaker.ianbot.BotCommands.*;

public class IanBot {
    public static MongoClient mongoClient;

    public static void main(String[] args) {
        String testServerId = "1112230651681308822";
        String fuzzyServerId = "270639436037881866";

        mongoClient = MongoClients.create(
                MongoClientSettings
                        .builder()
                        .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
                        .build()
        );

        JDA jda = null;
        try {
            jda = JDABuilder.createDefault(Tokens.ianBotToken)
                    .addEventListeners(new BotCommands())
                    .build().awaitReady();
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
        if (jda == null) {
            System.out.println("JDA was null, terminating");
            return;
        }

        Guild testGuild = jda.getGuildById(testServerId);
        if (testGuild != null) {
            testGuild.updateCommands().addCommands(getCommands()).queue();
        }
        Guild guild = jda.getGuildById(fuzzyServerId);
        if (guild != null) {
            guild.updateCommands().addCommands(getCommands()).queue();
        }
    }

    @NotNull
    private static List<CommandDataImpl> getCommands() {
        List<CommandDataImpl> commands = new ArrayList<>();
        CommandDataImpl ianTestCommand = new CommandDataImpl(IANTEST_COMMAND, "debug command");
        CommandDataImpl claimPointsCommand = new CommandDataImpl(CLAIM_POINTS_COMMAND, "claim free points once a day");
        commands.add(ianTestCommand);
        commands.add(claimPointsCommand);
        commands.add(getBlackjackCommand());
        commands.add(getSayCommand());
        return commands;
    }

    @NotNull
    private static CommandDataImpl getBlackjackCommand() {
        CommandDataImpl blackjackCommand = new CommandDataImpl(BLACKJACK_COMMAND, "play blackjack");
        OptionData blackjackOption = new OptionData(OptionType.STRING, "interaction", "blackjack interaction", true);
        blackjackOption.addChoice("new", "new");
        blackjackOption.addChoice("stats", "stats");
        blackjackOption.addChoice("hit", "hit");
        blackjackOption.addChoice("stand", "stand");
        blackjackOption.addChoice("double", "double");
        blackjackOption.addChoice("split", "split");
        blackjackCommand.addOptions(blackjackOption);
        return blackjackCommand;
    }

    @NotNull
    private static CommandDataImpl getSayCommand() {
        CommandDataImpl sayCommand = new CommandDataImpl(SAY_COMMAND, "say something");
        OptionData sayOption = new OptionData(OptionType.STRING, "say", "what to say", true);
        sayCommand.addOptions(sayOption);
        return sayCommand;
    }
}
