package com.ianscottbaker.ianbot.config;

import com.ianscottbaker.ianbot.BotCommands;
import com.ianscottbaker.ianbot.ButtonInteractions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;

@Configuration
public class JdaConfig {
    private static final Logger log = LoggerFactory.getLogger(JdaConfig.class);

    private final DiscordProperties discordProperties;

    public JdaConfig(DiscordProperties discordProperties) {
        this.discordProperties = discordProperties;
    }

    @Bean(destroyMethod = "shutdown")
    public JDA jda(BotCommands botCommands, ButtonInteractions buttonInteractions) throws InterruptedException {
        String token = discordProperties.getBotToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Discord bot token is not set. Provide it via the DISCORD_BOT_TOKEN environment variable, " +
                            "or copy .env.template to .env and fill it in.");
        }
        return JDABuilder.createDefault(token)
                .addEventListeners(botCommands, buttonInteractions)
                .build()
                .awaitReady();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerSlashCommands(ApplicationReadyEvent event) {
        JDA jda = event.getApplicationContext().getBean(JDA.class);
        List<CommandDataImpl> commands = buildCommands();
        registerForGuild(jda, discordProperties.getGuilds().getTestId(), commands, "test");
        registerForGuild(jda, discordProperties.getGuilds().getFuzzyId(), commands, "fuzzy");
    }

    private void registerForGuild(JDA jda, String guildId, List<CommandDataImpl> commands, String label) {
        if (guildId == null || guildId.isBlank()) {
            return;
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            log.warn("{} guild {} not found, skipping slash command registration", label, guildId);
            return;
        }
        guild.updateCommands().addCommands(commands).queue();
    }

    private List<CommandDataImpl> buildCommands() {
        return List.of(
                new CommandDataImpl(BotCommands.IANTEST_COMMAND, "debug command"),
                new CommandDataImpl(BotCommands.CLAIM_POINTS_COMMAND, "claim free points once a day"),
                buildBlackjackCommand(),
                buildSayCommand());
    }

    private CommandDataImpl buildBlackjackCommand() {
        CommandDataImpl blackjackCommand = new CommandDataImpl(BotCommands.BLACKJACK_COMMAND, "play blackjack");
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

    private CommandDataImpl buildSayCommand() {
        CommandDataImpl sayCommand = new CommandDataImpl(BotCommands.SAY_COMMAND, "say something");
        OptionData sayOption = new OptionData(OptionType.STRING, "say", "what to say", true);
        sayCommand.addOptions(sayOption);
        return sayCommand;
    }
}
