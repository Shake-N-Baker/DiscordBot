package com.ianscottbaker.ianbot.command.spire;

import com.ianscottbaker.ianbot.service.SpireService;

import java.util.Arrays;
import java.util.List;

/**
 * The full v1 card library: 12 attacks, 8 skills, 4 powers. Each constant owns
 * its full effect via {@code onPlay} (and {@code onTurnStart} for powers); the
 * engine only supplies primitives. The enum {@code name()} doubles as the card
 * ID persisted in {@code SpireRun.masterDeck}.
 */
public enum SpireCard {
    // --- Attacks ---
    STRIKE("Strike", 2, CardType.ATTACK, "Deal 6 damage to the front enemy.",
            (e, s) -> e.dealDamageToFront(s, 6)),
    HEAVY_STRIKE("Heavy Strike", 3, CardType.ATTACK, "Deal 14 damage to the front enemy.",
            (e, s) -> e.dealDamageToFront(s, 14)),
    BASH("Bash", 3, CardType.ATTACK, "Deal 8 damage to the front enemy and apply 2 Vulnerable.",
            (e, s) -> { e.applyVulnerableToFront(s, 2); e.dealDamageToFront(s, 8); }),
    CLEAVE("Cleave", 3, CardType.ATTACK, "Deal 8 damage to ALL enemies.",
            (e, s) -> e.dealDamageToAll(s, 8)),
    POMMEL_STRIKE("Pommel Strike", 2, CardType.ATTACK, "Deal 6 damage to the front enemy. Draw 1 card.",
            (e, s) -> { e.dealDamageToFront(s, 6); e.drawCards(s, 1); }),
    TWIN_STRIKE("Twin Strike", 2, CardType.ATTACK, "Deal 5 damage to the front enemy twice.",
            (e, s) -> { e.dealDamageToFront(s, 5); e.dealDamageToFront(s, 5); }),
    SNIPE("Snipe", 3, CardType.ATTACK, "Deal 12 damage to the BACK enemy.",
            (e, s) -> e.dealDamageToBack(s, 12)),
    CARNAGE("Carnage", 4, CardType.ATTACK, "Deal 18 damage to the front enemy.",
            (e, s) -> e.dealDamageToFront(s, 18)),
    WHIRLWIND("Whirlwind", 4, CardType.ATTACK, "Deal 6 damage to ALL enemies twice.",
            (e, s) -> { e.dealDamageToAll(s, 6); e.dealDamageToAll(s, 6); }),
    CRUSHING_BLOW("Crushing Blow", 4, CardType.ATTACK, "Deal 10 damage to the front enemy and apply 3 Vulnerable.",
            (e, s) -> { e.applyVulnerableToFront(s, 3); e.dealDamageToFront(s, 10); }),
    IRON_WAVE("Iron Wave", 2, CardType.ATTACK, "Deal 5 damage to the front enemy and gain 5 Block.",
            (e, s) -> { e.dealDamageToFront(s, 5); e.gainBlock(s, 5); }),
    DROPKICK("Dropkick", 2, CardType.ATTACK, "Deal 8 damage to the front enemy. If it is Vulnerable, gain 1 energy and draw 1 card.",
            SpireService::dropkick),

    // --- Skills ---
    DEFEND("Defend", 2, CardType.SKILL, "Gain 5 Block.",
            (e, s) -> e.gainBlock(s, 5)),
    HEAVY_DEFEND("Heavy Defend", 3, CardType.SKILL, "Gain 11 Block.",
            (e, s) -> e.gainBlock(s, 11)),
    SHRUG_IT_OFF("Shrug It Off", 2, CardType.SKILL, "Gain 6 Block. Draw 1 card.",
            (e, s) -> { e.gainBlock(s, 6); e.drawCards(s, 1); }),
    ENTRENCH("Entrench", 3, CardType.SKILL, "Double your current Block.",
            SpireService::doubleBlock),
    INTIMIDATE("Intimidate", 1, CardType.SKILL, "Apply 1 Weak to ALL enemies.",
            (e, s) -> e.applyWeakToAll(s, 1)),
    TERROR("Terror", 1, CardType.SKILL, "Apply 3 Vulnerable to the front enemy.",
            (e, s) -> e.applyVulnerableToFront(s, 3)),
    DISARM("Disarm", 2, CardType.SKILL, "The front enemy loses 2 Strength.",
            (e, s) -> e.enemyFrontLoseStrength(s, 2)),
    WAR_CRY("War Cry", 0, CardType.SKILL, "Draw 2 cards.",
            (e, s) -> e.drawCards(s, 2)),

    // --- Powers ---
    INFLAME("Inflame", 2, CardType.POWER, "Gain 3 Strength.",
            (e, s) -> e.gainStrength(s, 3)),
    METALLICIZE("Metallicize", 3, CardType.POWER, "At the start of each turn, gain 4 Block.",
            (e, s) -> { }, (e, s) -> e.gainBlock(s, 4)),
    DEMON_FORM("Demon Form", 6, CardType.POWER, "At the start of each turn, gain 2 Strength.",
            (e, s) -> { }, (e, s) -> e.gainStrength(s, 2)),
    BERSERK("Berserk", 2, CardType.POWER, "At the start of each turn, gain 2 energy.",
            (e, s) -> { }, (e, s) -> e.gainEnergy(s, 2));

    public enum CardType {
        ATTACK,
        SKILL,
        POWER
    }

    /** A composable card effect. The engine is passed in so cards never hold state. */
    @FunctionalInterface
    public interface CardEffect {
        void apply(SpireService engine, CombatState state);
    }

    private final String displayName;
    private final int cost;
    private final CardType type;
    private final String text;
    private final CardEffect onPlay;
    private final CardEffect onTurnStart;

    SpireCard(String displayName, int cost, CardType type, String text, CardEffect onPlay) {
        this(displayName, cost, type, text, onPlay, null);
    }

    SpireCard(String displayName, int cost, CardType type, String text, CardEffect onPlay, CardEffect onTurnStart) {
        this.displayName = displayName;
        this.cost = cost;
        this.type = type;
        this.text = text;
        this.onPlay = onPlay;
        this.onTurnStart = onTurnStart;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCost() {
        return cost;
    }

    public CardType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public CardEffect getOnPlay() {
        return onPlay;
    }

    public CardEffect getOnTurnStart() {
        return onTurnStart;
    }

    /** Cards eligible to appear in post-combat reward offers — every card except the basics. */
    public static List<SpireCard> rewardPool() {
        return Arrays.stream(values())
                .filter(c -> c != STRIKE && c != DEFEND)
                .toList();
    }
}
