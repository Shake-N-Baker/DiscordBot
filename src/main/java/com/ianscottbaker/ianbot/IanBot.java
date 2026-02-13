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
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ianscottbaker.ianbot.BotCommands.*;

public class IanBot {
    public static MongoClient mongoClient;
    private static final Logger logger = LoggerFactory.getLogger(IanBot.class);

    public static void main(String[] args) {
        String testServerId = "1112230651681308822";
        String fuzzyServerId = "270639436037881866";
        List<String> serverIds = new ArrayList<>(Arrays.asList(fuzzyServerId, testServerId));

        // Default codec registry to serialize / deserialize between BSON and POJO. Add custom serialization codecs below
        // https://www.mongodb.com/docs/drivers/java/sync/v5.2/fundamentals/data-formats/codecs/#default-codec-registry
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry());
        mongoClient = MongoClients.create(
                MongoClientSettings
                        .builder()
                        .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
                        .codecRegistry(pojoCodecRegistry)
                        .build()
        );

        JDA jda;
        try {
            jda = JDABuilder.createDefault(Tokens.IAN_BOT_TOKEN)
                    .addEventListeners(new BotCommands(), new ButtonInteractions())
                    .build().awaitReady();
        } catch (InterruptedException interruptedException) {
            logger.error("An error occurred while initializing JDA", interruptedException);
            return;
        }

        for (String serverId : serverIds) {
            Guild guild = jda.getGuildById(serverId);
            if (guild != null) {
                guild.updateCommands().addCommands(getCommands()).queue();
            } else {
                logger.warn("Could not find serverId: {}", serverId);
            }
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
