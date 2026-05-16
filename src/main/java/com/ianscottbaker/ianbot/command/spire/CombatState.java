package com.ianscottbaker.ianbot.command.spire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Live combat state — embedded in {@link SpireRun} only while the run's phase is
 * COMBAT. Player HP lives on the run, not here. The three card piles and the
 * enemy list are mutated turn by turn; the master deck is never touched.
 */
public class CombatState {
    private long seed;
    private int energy;
    private int turnNumber;
    private List<SpireCard> hand = new ArrayList<>();
    private List<SpireCard> drawPile = new ArrayList<>();
    private List<SpireCard> discardPile = new ArrayList<>();
    private List<SpireCard> activePowers = new ArrayList<>();
    private Map<StatusEffect, Integer> playerEffects = new HashMap<>();
    private List<EnemyInstance> enemies = new ArrayList<>();
    private List<String> lastTurnLog = new ArrayList<>();

    public CombatState() {
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getEnergy() {
        return energy;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public List<SpireCard> getHand() {
        return hand;
    }

    public void setHand(List<SpireCard> hand) {
        this.hand = hand;
    }

    public List<SpireCard> getDrawPile() {
        return drawPile;
    }

    public void setDrawPile(List<SpireCard> drawPile) {
        this.drawPile = drawPile;
    }

    public List<SpireCard> getDiscardPile() {
        return discardPile;
    }

    public void setDiscardPile(List<SpireCard> discardPile) {
        this.discardPile = discardPile;
    }

    public List<SpireCard> getActivePowers() {
        return activePowers;
    }

    public void setActivePowers(List<SpireCard> activePowers) {
        this.activePowers = activePowers;
    }

    public Map<StatusEffect, Integer> getPlayerEffects() {
        return playerEffects;
    }

    public void setPlayerEffects(Map<StatusEffect, Integer> playerEffects) {
        this.playerEffects = playerEffects;
    }

    public List<EnemyInstance> getEnemies() {
        return enemies;
    }

    public void setEnemies(List<EnemyInstance> enemies) {
        this.enemies = enemies;
    }

    public List<String> getLastTurnLog() {
        return lastTurnLog;
    }

    public void setLastTurnLog(List<String> lastTurnLog) {
        this.lastTurnLog = lastTurnLog;
    }
}
