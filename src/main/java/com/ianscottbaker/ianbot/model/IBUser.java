package com.ianscottbaker.ianbot.model;

import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.Serializable;

public class IBUser implements Serializable {
    private String discordId;
    private IBBank bank;
    private int points;
    private int lastFreeClaimTime;
    // need to track previous bot message to erase with any new replies

    // New user constructor
    public IBUser(String newDiscordId) {
        this.discordId = newDiscordId;
        this.bank = new IBBank();
        this.points = 0;
        this.lastFreeClaimTime = 0;
    }

    // From document constructor
    public IBUser(Document document) {
        this.discordId = document.getString("discordId");
        Document bankDoc = document.get("bank", Document.class);
        this.bank = bankDoc != null ? new IBBank(bankDoc) : new IBBank();
        this.points = document.getInteger("points", 0);
        this.lastFreeClaimTime = document.getInteger("lastFreeClaimTime", 0);
    }

    public Bson getDatabaseUpdates() {
        return Updates.combine(
                Updates.set("discordId", this.discordId),
                Updates.set("points", this.points),
                Updates.set("lastFreeClaimTime", this.lastFreeClaimTime),
                Updates.set("bank", this.bank.toDocument())
        );
    }

    // Getters and setters
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
