package com.ianscottbaker.ianbot.command;

import com.ianscottbaker.ianbot.model.IBUser;
import com.ianscottbaker.ianbot.service.UserService;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
public class ClaimPoints {
    public static final int MIN_CLAIM_POINTS = 5;
    public static final int MAX_CLAIM_POINTS = 10;

    private final UserService userService;
    private final Random random = new Random();

    public ClaimPoints(UserService userService) {
        this.userService = userService;
    }

    public void execute(@NotNull SlashCommandInteractionEvent event, @NotNull IBUser ibUser) {
        int currentTime = (int) Instant.now().getEpochSecond();
        int lastTime = ibUser.getLastFreeClaimTime();
        if (currentTime - lastTime < 86400) {
            event.reply("You can not claim any more points today, try again tomorrow!").setEphemeral(true).queue();
            return;
        }

        int claimPoints = random.nextInt(MAX_CLAIM_POINTS - MIN_CLAIM_POINTS) + MIN_CLAIM_POINTS;
        int newPoints = ibUser.getPoints() + claimPoints;

        ibUser.setPoints(newPoints);
        ibUser.setLastFreeClaimTime(currentTime);
        userService.save(ibUser);

        event.reply(String.format("You've claimed %d points! You now have %d points", claimPoints, newPoints)).setEphemeral(true).queue();
    }
}
