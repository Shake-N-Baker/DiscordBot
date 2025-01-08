package com.ianscottbaker.ianbot.command;

import com.ianscottbaker.ianbot.model.IBUser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Random;

public class ClaimPoints {
    public static int MIN_CLAIM_POINTS = 5;
    public static int MAX_CLAIM_POINTS = 10;

    public static void execute(@NotNull SlashCommandInteractionEvent event,
                               @NotNull IBUser ibUser,
                               @NotNull MongoCollection<Document> userCollection,
                               Document updateQuery,
                               UpdateOptions updateOptions
    ) {
        int currentTime = (int) Instant.now().getEpochSecond();
        int lastTime = ibUser.getLastFreeClaimTime();
        if (currentTime - lastTime < 86400) {
            event.reply("You can not claim any more points today, try again tomorrow!").setEphemeral(true).queue();
            return;
        }

        int currentPoints = ibUser.getPoints();
        Random random = new Random();
        int claimPoints = random.nextInt(MAX_CLAIM_POINTS - MIN_CLAIM_POINTS) + MIN_CLAIM_POINTS;
        int newPoints = currentPoints + claimPoints;

        ibUser.setPoints(newPoints);
        ibUser.setLastFreeClaimTime(currentTime);
        Bson updates = ibUser.getDatabaseUpdates();
        userCollection.updateOne(updateQuery, updates, updateOptions);

        event.reply(String.format("You've claimed %d points! You now have %d points", claimPoints, newPoints)).setEphemeral(true).queue();
    }
}
