package com.ianscottbaker.ianbot.command.spire;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Root document for a player's Slay-the-Spire-like run. One active run per user
 * (unique index on {@code discordId}). All state is serialized here on every
 * action so a bot restart resumes mid-fight exactly where it left off.
 */
@Document(collection = "spireRun")
public class SpireRun {
    @Id
    private String id;

    @Indexed(unique = true)
    private String discordId;

    private long seed;
    private int currentFloor;
    private Phase phase;
    private int playerHp;
    private int playerMaxHp;
    private List<String> masterDeck = new ArrayList<>();
    private CombatState combatState;
    private List<String> currentRewardOffer;
    private String currentMessageId;
    private String currentChannelId;
    private Instant createdAt;
    private Instant updatedAt;

    public SpireRun() {
    }

    public SpireRun(String discordId) {
        this.discordId = discordId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public int getPlayerHp() {
        return playerHp;
    }

    public void setPlayerHp(int playerHp) {
        this.playerHp = playerHp;
    }

    public int getPlayerMaxHp() {
        return playerMaxHp;
    }

    public void setPlayerMaxHp(int playerMaxHp) {
        this.playerMaxHp = playerMaxHp;
    }

    public List<String> getMasterDeck() {
        return masterDeck;
    }

    public void setMasterDeck(List<String> masterDeck) {
        this.masterDeck = masterDeck;
    }

    public CombatState getCombatState() {
        return combatState;
    }

    public void setCombatState(CombatState combatState) {
        this.combatState = combatState;
    }

    public List<String> getCurrentRewardOffer() {
        return currentRewardOffer;
    }

    public void setCurrentRewardOffer(List<String> currentRewardOffer) {
        this.currentRewardOffer = currentRewardOffer;
    }

    public String getCurrentMessageId() {
        return currentMessageId;
    }

    public void setCurrentMessageId(String currentMessageId) {
        this.currentMessageId = currentMessageId;
    }

    public String getCurrentChannelId() {
        return currentChannelId;
    }

    public void setCurrentChannelId(String currentChannelId) {
        this.currentChannelId = currentChannelId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
