package com.ianscottbaker.ianbot.model;

import org.bson.Document;

import java.io.Serializable;

public class IBBank implements Serializable {
    private int points;
    private int lastFreeClaimTime;

    // New constructor
    public IBBank() {
        this.points = 0;
        this.lastFreeClaimTime = 0;
    }

    // From document constructor
    public IBBank(Document document) {
        this.points = document.getInteger("points", 0);
        this.lastFreeClaimTime = document.getInteger("lastFreeClaimTime", 0);
    }

    public Document toDocument() {
        return new Document("points", points)
                .append("lastFreeClaimTime", lastFreeClaimTime);
    }

    // Getters and setters
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
