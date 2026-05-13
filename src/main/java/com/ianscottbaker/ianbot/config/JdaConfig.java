package com.ianscottbaker.ianbot.config;

import com.ianscottbaker.ianbot.BotCommands;
import com.ianscottbaker.ianbot.ButtonInteractions;
import com.ianscottbaker.ianbot.ModalInteractions;
import com.ianscottbaker.ianbot.command.SlashCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
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
    private final List<SlashCommand> commands;

    public JdaConfig(DiscordProperties discordProperties, List<SlashCommand> commands) {
        this.discordProperties = discordProperties;
        this.commands = commands;
    }

    @Bean(destroyMethod = "shutdown")
    public JDA jda(BotCommands botCommands, ButtonInteractions buttonInteractions, ModalInteractions modalInteractions) throws InterruptedException {
        String token = resolveDiscordToken();
        return JDABuilder.createDefault(token)
                .addEventListeners(botCommands, buttonInteractions, modalInteractions)
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
        List<SlashCommandData> commandData = commands.stream().map(SlashCommand::getCommandData).toList();
        registerForGuild(jda, discordProperties.getGuilds().getTestId(), commandData, "test");
        registerForGuild(jda, discordProperties.getGuilds().getFuzzyId(), commandData, "fuzzy");
    }

    private void registerForGuild(JDA jda, String guildId, List<SlashCommandData> commandData, String label) {
        if (guildId == null || guildId.isBlank()) {
            return;
        }
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            log.warn("{} guild {} not found, skipping slash command registration", label, guildId);
            return;
        }
        guild.updateCommands().addCommands(commandData).queue();
    }
}
