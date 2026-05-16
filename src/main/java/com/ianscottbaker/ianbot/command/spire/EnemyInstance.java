package com.ianscottbaker.ianbot.command.spire;

import java.util.HashMap;
import java.util.Map;

/**
 * One enemy's live combat state. Embedded inside {@link CombatState}; needs a
 * no-arg constructor and bean accessors for Mongo serialization.
 */
public class EnemyInstance {
    private SpireEnemy enemyType;
    private int currentHp;
    private int maxHp;
    private Map<StatusEffect, Integer> effects = new HashMap<>();
    private int intentIndex;
    private Intent currentIntent;

    public EnemyInstance() {
    }

    public EnemyInstance(SpireEnemy enemyType) {
        this.enemyType = enemyType;
        this.maxHp = enemyType.getMaxHp();
        this.currentHp = enemyType.getMaxHp();
    }

    public boolean isAlive() {
        return currentHp > 0;
    }

    public SpireEnemy getEnemyType() {
        return enemyType;
    }

    public void setEnemyType(SpireEnemy enemyType) {
        this.enemyType = enemyType;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = currentHp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public Map<StatusEffect, Integer> getEffects() {
        return effects;
    }

    public void setEffects(Map<StatusEffect, Integer> effects) {
        this.effects = effects;
    }

    public int getIntentIndex() {
        return intentIndex;
    }

    public void setIntentIndex(int intentIndex) {
        this.intentIndex = intentIndex;
    }

    public Intent getCurrentIntent() {
        return currentIntent;
    }

    public void setCurrentIntent(Intent currentIntent) {
        this.currentIntent = currentIntent;
    }
}
