package com.ianscottbaker.ianbot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user")
public class IBUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String discordId;
    private int points;
    private int lastFreeClaimTime;

    // New user constructor
    public IBUser(String newDiscordId) {
        this.discordId = newDiscordId;
        this.points = 0;
        this.lastFreeClaimTime = 0;
    }

    public String getId() {
        return id;
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
