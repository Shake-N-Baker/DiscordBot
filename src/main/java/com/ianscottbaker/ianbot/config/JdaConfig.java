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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

@Configuration
public class JdaConfig {
    private static final Logger log = LoggerFactory.getLogger(JdaConfig.class);
    private static final String TOKEN_KEY = "DISCORD_BOT_TOKEN";
    private static final Path DOTENV_PATH = Path.of(".env");

    private final DiscordProperties discordProperties;

    public JdaConfig(DiscordProperties discordProperties) {
        this.discordProperties = discordProperties;
    }

    @Bean(destroyMethod = "shutdown")
    public JDA jda(BotCommands botCommands, ButtonInteractions buttonInteractions) throws InterruptedException {
        String token = resolveDiscordToken();
        return JDABuilder.createDefault(token)
                .addEventListeners(botCommands, buttonInteractions)
                .build()
                .awaitReady();
    }

    private String resolveDiscordToken() {
        String envToken = System.getenv(TOKEN_KEY);
        if (envToken != null && !envToken.isBlank()) {
            log.info("Discord token resolved from environment variable {}", TOKEN_KEY);
            return envToken;
        }

        Path envPath = DOTENV_PATH.toAbsolutePath();
        if (Files.isReadable(envPath)) {
            Properties props = new Properties();
            try (InputStream in = Files.newInputStream(envPath)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read " + envPath + ": " + e.getMessage(), e);
            }
            String fileToken = props.getProperty(TOKEN_KEY);
            if (fileToken != null && !fileToken.isBlank()) {
                log.info("Discord token resolved from {}", envPath);
                return fileToken;
            }
        }

        throw new IllegalStateException(
                "Discord bot token is not set. Provide it via the " + TOKEN_KEY +
                        " environment variable, or copy .env.template to .env (at " + envPath + ") and fill in " + TOKEN_KEY + ".");
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
