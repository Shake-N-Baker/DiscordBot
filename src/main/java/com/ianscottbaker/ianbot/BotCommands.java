package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.command.Blackjack;
import com.ianscottbaker.ianbot.command.ClaimPoints;
import com.ianscottbaker.ianbot.model.IBUser;
import com.ianscottbaker.ianbot.service.UserService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BotCommands extends ListenerAdapter {
    public static final String IANTEST_COMMAND = "iantest";
    public static final String SAY_COMMAND = "say";
    public static final String CLAIM_POINTS_COMMAND = "claim_points";
    public static final String BLACKJACK_COMMAND = "blackjack";

    private final UserService userService;
    private final Blackjack blackjack;
    private final ClaimPoints claimPoints;

    public BotCommands(UserService userService, Blackjack blackjack, ClaimPoints claimPoints) {
        this.userService = userService;
        this.blackjack = blackjack;
        this.claimPoints = claimPoints;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String eventUserName = event.getUser().getGlobalName();
        String eventUserId = event.getUser().getId();

        if (event.getName().equals(IANTEST_COMMAND)) {
            Optional<IBUser> existing = userService.findExisting(eventUserId);
            IBUser ibUser = existing.orElseGet(() -> new IBUser(eventUserId));
            String suffix = existing.isPresent() ? "existing" : "new";
            event.reply(String.format(
                    "name: %s, id: %s, points: %d, lastFreeClaimTime: %d (%s user)",
                    eventUserName, eventUserId, ibUser.getPoints(), ibUser.getLastFreeClaimTime(), suffix
            )).setEphemeral(true).queue();
            return;
        } else if (event.getName().equals(SAY_COMMAND)) {
            OptionMapping sayMapping = event.getOption("say");
            if (sayMapping == null) {
                event.reply("Please provide a valid say command").setEphemeral(true).queue();
                return;
            }
            event.reply(sayMapping.getAsString()).setEphemeral(false).queue();
            return;
        }

        IBUser ibUser = userService.getOrCreate(eventUserId);
        if (event.getName().equals(CLAIM_POINTS_COMMAND)) {
            claimPoints.execute(event, ibUser);
            return;
        } else if (event.getName().equals(BLACKJACK_COMMAND)) {
            blackjack.execute(event, ibUser);
            return;
        }
        event.reply("Something went wrong!").setEphemeral(true).queue();
    }
}
