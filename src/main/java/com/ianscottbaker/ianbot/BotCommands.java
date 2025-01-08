package com.ianscottbaker.ianbot;

import com.ianscottbaker.ianbot.command.Blackjack;
import com.ianscottbaker.ianbot.command.ClaimPoints;
import com.ianscottbaker.ianbot.model.IBUser;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import static com.mongodb.client.model.Filters.eq;

public class BotCommands extends ListenerAdapter {
    public static String IANTEST_COMMAND = "iantest";
    public static String CLAIM_POINTS_COMMAND = "claim_points";
    public static String BLACKJACK_COMMAND = "blackjack";

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String eventUserName = event.getUser().getGlobalName();
        String eventUserId = event.getUser().getId();
        MongoCollection<Document> userCollection = IanBot.mongoClient.getDatabase("ianbot").getCollection("user");
        Document rawDatabaseUser = userCollection.find(eq("discordId", eventUserId)).first();
        IBUser ibUser = null;
        if (rawDatabaseUser == null) {
            // No user found, insert a new user in the database
            ibUser = new IBUser(eventUserId);
        } else {
            ibUser = new IBUser(rawDatabaseUser);
        }
        // Query and options for updating/inserting user in/into database
        Document updateQuery = new Document().append("discordId", eventUserId);
        // Instructs the driver to insert a new document if none match the query
        UpdateOptions updateOptions = new UpdateOptions().upsert(true);

        if (event.getName().equals(IANTEST_COMMAND)) {
            if (rawDatabaseUser == null) {
                event.reply(String.format("name: %s, id: %s, points: %d", eventUserName, eventUserId, ibUser.getPoints())).setEphemeral(true).queue();
            } else {
                event.reply(String.format("name: %s, id: %s, points: %d, mongoDB JSON: %s", eventUserName, eventUserId, ibUser.getPoints(), rawDatabaseUser.toJson())).setEphemeral(true).queue();
            }
            return;
        } else
            if (event.getName().equals(CLAIM_POINTS_COMMAND)) {
            ClaimPoints.execute(event, ibUser, userCollection, updateQuery, updateOptions);
            return;
        } else if (event.getName().equals(BLACKJACK_COMMAND)) {
            Blackjack.execute(event, ibUser, userCollection, updateQuery, updateOptions);
            return;
        }
        event.reply("Something went wrong!").setEphemeral(true).queue();
    }
}
