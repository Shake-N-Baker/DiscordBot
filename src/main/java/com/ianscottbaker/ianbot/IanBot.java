package com.ianscottbaker.ianbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

import static com.ianscottbaker.ianbot.BotCommands.*;

@SpringBootApplication
public class IanBot {

    @Value("${discord.token}")
    private String discordToken;

    public static void main(String[] args) {
        SpringApplication.run(IanBot.class, args);
    }

    @Bean
    public JDA jda(BotCommands botCommands, ButtonInteractions buttonInteractions) throws InterruptedException {
        String testServerId = "1112230651681308822";
        String fuzzyServerId = "270639436037881866";

        JDA jda = JDABuilder.createDefault(discordToken)
                .addEventListeners(botCommands, buttonInteractions)
                .build()
                .awaitReady();

        Guild testGuild = jda.getGuildById(testServerId);
        if (testGuild != null) {
            testGuild.updateCommands().addCommands(getCommands()).queue();
        }
        Guild fuzzyGuild = jda.getGuildById(fuzzyServerId);
        if (fuzzyGuild != null) {
            fuzzyGuild.updateCommands().addCommands(getCommands()).queue();
        }

        return jda;
    }

    @NotNull
    private static List<CommandDataImpl> getCommands() {
        List<CommandDataImpl> commands = new ArrayList<>();
        commands.add(new CommandDataImpl(IANTEST_COMMAND, "debug command"));
        commands.add(new CommandDataImpl(CLAIM_POINTS_COMMAND, "claim free points once a day"));
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
