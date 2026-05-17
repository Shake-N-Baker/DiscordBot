package com.ianscottbaker.ianbot.command.spire;

import java.util.List;
import java.util.Random;

/**
 * Enemy definitions and intent logic. Regular enemies cycle a deterministic
 * {@code script}; the boss ({@link #TADPOLE_THE_TERRIBLE}) ignores its script and uses
 * {@link #bossIntent} — weighted-random across two HP-gated phases.
 */
public enum SpireEnemy {
    // --- Easy tier (floors 1-3) ---
    CULTIST("Cultist", 28, Tier.EASY, List.of(
            Intent.of(Intent.Type.BUFF_STRENGTH, 2),
            Intent.of(Intent.Type.ATTACK, 6),
            Intent.of(Intent.Type.ATTACK, 6))),
    BAT("Bat", 16, Tier.EASY, List.of(
            Intent.of(Intent.Type.ATTACK, 6),
            Intent.of(Intent.Type.BLOCK, 6))),
    ACID_SLIME("Acid Slime", 24, Tier.EASY, List.of(
            Intent.of(Intent.Type.ATTACK, 7),
            Intent.of(Intent.Type.HEAL, 5),
            Intent.of(Intent.Type.APPLY_WEAK, 1))),

    // --- Hard tier (floors 5-7) ---
    SLAVER("Slaver", 32, Tier.HARD, List.of(
            Intent.of(Intent.Type.ATTACK, 8),
            Intent.of(Intent.Type.APPLY_VULNERABLE, 1),
            Intent.of(Intent.Type.ATTACK, 8),
            Intent.of(Intent.Type.APPLY_WEAK, 1))),
    SENTRY("Sentry", 30, Tier.HARD, List.of(
            Intent.of(Intent.Type.BLOCK, 8),
            Intent.of(Intent.Type.WINDUP, 0),
            Intent.of(Intent.Type.NUKE, 22))),
    BRUTE("Brute", 41, Tier.HARD, List.of(
            Intent.of(Intent.Type.BUFF_STRENGTH, 3),
            Intent.of(Intent.Type.ATTACK, 10),
            Intent.of(Intent.Type.ATTACK, 10))),

    // --- Boss (floor 9) ---
    TADPOLE_THE_TERRIBLE("Tadpole The Terrible", 100, Tier.BOSS, List.of());

    public enum Tier {
        EASY,
        HARD,
        BOSS
    }

    private static final List<Intent> BOSS_PHASE_1 = List.of(
            Intent.of(Intent.Type.ATTACK, 14),
            Intent.of(Intent.Type.BLOCK, 12),
            Intent.of(Intent.Type.APPLY_VULNERABLE, 3));

    private static final List<Intent> BOSS_PHASE_2 = List.of(
            Intent.of(Intent.Type.ATTACK, 20),
            Intent.of(Intent.Type.BUFF_STRENGTH, 3),
            Intent.of(Intent.Type.WINDUP, 0));

    /** Boss enrages at or below this HP, switching to the phase-2 rotation. */
    public static final int BOSS_ENRAGE_HP = 50;
    private static final int BOSS_NUKE_DAMAGE = 30;

    private final String displayName;
    private final int maxHp;
    private final Tier tier;
    private final List<Intent> script;

    SpireEnemy(String displayName, int maxHp, Tier tier, List<Intent> script) {
        this.displayName = displayName;
        this.maxHp = maxHp;
        this.tier = tier;
        this.script = script;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public Tier getTier() {
        return tier;
    }

    public List<Intent> getScript() {
        return script;
    }

    public boolean isBoss() {
        return tier == Tier.BOSS;
    }

    /**
     * Next boss intent. A windup is always followed by the inferno nuke; otherwise
     * a weighted pick from the active phase pool, avoiding an immediate repeat.
     */
    public static Intent bossIntent(Intent justResolved, boolean phase2, Random rng) {
        if (justResolved != null && justResolved.getType() == Intent.Type.WINDUP) {
            return Intent.of(Intent.Type.NUKE, BOSS_NUKE_DAMAGE);
        }
        List<Intent> pool = phase2 ? BOSS_PHASE_2 : BOSS_PHASE_1;
        Intent pick = pool.get(rng.nextInt(pool.size()));
        int attempts = 0;
        while (justResolved != null && pick.getType() == justResolved.getType() && attempts < 4) {
            pick = pool.get(rng.nextInt(pool.size()));
            attempts++;
        }
        return Intent.of(pick.getType(), pick.getValue());
    }
}
