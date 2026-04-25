package com.ianscottbaker.ianbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static com.ianscottbaker.ianbot.BotCommands.*;

@SpringBootApplication
public class IanBot implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private BotCommands botCommands;

    @Autowired
    private ButtonInteractions buttonInteractions;

    private JDA jda;

    public static final String TEST_SERVER_ID = "1112230651681308822";
    public static final String FUZZY_SERVER_ID = "270639436037881866";

    @PostConstruct
    public void initialize() {
        // This method will be called after Spring has initialized all beans
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            jda = JDABuilder.createDefault(Tokens.ianBotToken)
                    .addEventListeners(botCommands, buttonInteractions)
                    .build().awaitReady();

            Guild testGuild = jda.getGuildById(TEST_SERVER_ID);
            if (testGuild != null) {
                testGuild.updateCommands().addCommands(getCommands()).queue();
            }
            Guild guild = jda.getGuildById(FUZZY_SERVER_ID);
            if (guild != null) {
                guild.updateCommands().addCommands(getCommands()).queue();
            }
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }

    private List<CommandDataImpl> getCommands() {
        List<CommandDataImpl> commands = new ArrayList<>();
        CommandDataImpl ianTestCommand = new CommandDataImpl(IANTEST_COMMAND, "debug command");
        CommandDataImpl claimPointsCommand = new CommandDataImpl(CLAIM_POINTS_COMMAND, "claim free points once a day");
        commands.add(ianTestCommand);
        commands.add(claimPointsCommand);
        commands.add(getBlackjackCommand());
        commands.add(getSayCommand());
        return commands;
    }

    private CommandDataImpl getBlackjackCommand() {
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

    private CommandDataImpl getSayCommand() {
        CommandDataImpl sayCommand = new CommandDataImpl(SAY_COMMAND, "say something");
        OptionData sayOption = new OptionData(OptionType.STRING, "say", "what to say", true);
        sayCommand.addOptions(sayOption);
        return sayCommand;
    }
}
