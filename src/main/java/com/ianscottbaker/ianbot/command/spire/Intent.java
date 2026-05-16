package com.ianscottbaker.ianbot.command.spire;

/**
 * A single telegraphed enemy action. Computed at the end of each enemy turn and
 * stored on the {@link EnemyInstance} so the player sees it during their turn.
 * Embedded inside {@code CombatState}; needs a no-arg constructor for Mongo.
 */
public class Intent {
    public enum Type {
        ATTACK,
        BLOCK,
        BUFF_STRENGTH,
        APPLY_VULNERABLE,
        APPLY_WEAK,
        HEAL,
        WINDUP,
        NUKE
    }

    private Type type;
    private int value;

    public Intent() {
    }

    public Intent(Type type, int value) {
        this.type = type;
        this.value = value;
    }

    public static Intent of(Type type, int value) {
        return new Intent(type, value);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
