package com.ianscottbaker.ianbot.model;

import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.Serializable;

public class IBUser implements Serializable {
    private String discordId;
    private int points;
    private int lastFreeClaimTime;

    // New user constructor
    public IBUser(String newDiscordId) {
        this.discordId = newDiscordId;
        this.points = 0;
        this.lastFreeClaimTime = 0;
    }

    public IBUser(Document document) {
        this.discordId = document.getString("discordId");
        this.points = document.getInteger("points", 0);
        this.lastFreeClaimTime = document.getInteger("lastFreeClaimTime", 0);
    }

    public Bson getDatabaseUpdates() {
        return Updates.combine(
                Updates.set("discordId", this.discordId),
                Updates.set("points", this.points),
                Updates.set("lastFreeClaimTime", this.lastFreeClaimTime)
        );
    }

    public String getDiscordId() {
        return discordId;
    }
    public void setDiscordId(String value) {
        this.discordId = value;
    }
    public int getPoints() {
        return points;
    }
    public void setPoints(int value) {
        this.points = value;
    }
    public int getLastFreeClaimTime() {
        return lastFreeClaimTime;
    }
    public void setLastFreeClaimTime(int value) {
        this.lastFreeClaimTime = value;
    }
}
