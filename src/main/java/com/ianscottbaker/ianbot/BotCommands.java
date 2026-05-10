package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.command.Blackjack;
import com.ianscottbaker.ianbot.command.ClaimPoints;
import com.ianscottbaker.ianbot.model.IBUser;
import com.ianscottbaker.ianbot.repository.IBUserRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class BotCommands extends ListenerAdapter {
    public static final String IANTEST_COMMAND = "iantest";
    public static final String SAY_COMMAND = "say";
    public static final String CLAIM_POINTS_COMMAND = "claim_points";
    public static final String BLACKJACK_COMMAND = "blackjack";

    private final IBUserRepository userRepository;
    private final ClaimPoints claimPoints;
    private final Blackjack blackjack;

    public BotCommands(IBUserRepository userRepository, ClaimPoints claimPoints, Blackjack blackjack) {
        this.userRepository = userRepository;
        this.claimPoints = claimPoints;
        this.blackjack = blackjack;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String eventUserName = event.getUser().getGlobalName();
        String eventUserId = event.getUser().getId();

        IBUser ibUser = userRepository.findByDiscordId(eventUserId)
                .orElseGet(() -> new IBUser(eventUserId));

        if (event.getName().equals(IANTEST_COMMAND)) {
            event.reply(String.format("name: %s, id: %s, points: %d", eventUserName, eventUserId, ibUser.getPoints()))
                    .setEphemeral(true).queue();

        } else if (event.getName().equals(SAY_COMMAND)) {
            OptionMapping sayMapping = event.getOption("say");
            if (sayMapping == null) {
                event.reply("Please provide a valid say command").setEphemeral(true).queue();
                return;
            }
            event.reply(sayMapping.getAsString()).setEphemeral(false).queue();

        } else if (event.getName().equals(CLAIM_POINTS_COMMAND)) {
            claimPoints.execute(event, ibUser, userRepository);

        } else if (event.getName().equals(BLACKJACK_COMMAND)) {
            blackjack.execute(event, ibUser, userRepository);

        } else {
            event.reply("Something went wrong!").setEphemeral(true).queue();
        }
    }
}
