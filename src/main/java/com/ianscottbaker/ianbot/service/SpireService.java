package com.ianscottbaker.ianbot.service;

import com.ianscottbaker.ianbot.command.spire.CombatState;
import com.ianscottbaker.ianbot.command.spire.EnemyInstance;
import com.ianscottbaker.ianbot.command.spire.Floor;
import com.ianscottbaker.ianbot.command.spire.Intent;
import com.ianscottbaker.ianbot.command.spire.Phase;
import com.ianscottbaker.ianbot.command.spire.SpireCard;
import com.ianscottbaker.ianbot.command.spire.SpireEnemy;
import com.ianscottbaker.ianbot.command.spire.SpireRun;
import com.ianscottbaker.ianbot.command.spire.StatusEffect;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The Spire game engine: run setup, the card-play / turn loop, enemy resolution,
 * and floor/phase advancement. Stateless — every method takes the {@link SpireRun}
 * (and its embedded {@link CombatState}) it operates on. The caller is responsible
 * for persisting the run after each call.
 */
@Service
public class SpireService {
    public static final int PLAYER_MAX_HP = 80;
    public static final int ENERGY_PER_TURN = 6;
    public static final int CARDS_PER_TURN = 4;
    public static final int MAX_HAND_SIZE = 8;
    public static final int REWARD_OFFER_SIZE = 3;
    public static final int REST_HEAL_PERCENT = 30;
    public static final int BOSS_REWARD_POINTS = 50;

    /** Distinct decision kinds, mixed into the run seed so sub-streams are stable across restarts. */
    private enum Decision {
        COMBAT_SHUFFLE,
        ENEMY_COMP,
        REWARD,
        BOSS_INTENT
    }

    // ------------------------------------------------------------------
    // Run lifecycle
    // ------------------------------------------------------------------

    public SpireRun newRun(String discordId) {
        SpireRun run = new SpireRun(discordId);
        run.setSeed(new SecureRandom().nextLong());
        run.setPlayerMaxHp(PLAYER_MAX_HP);
        run.setPlayerHp(PLAYER_MAX_HP);
        run.setCurrentFloor(1);
        run.setMasterDeck(starterDeck());
        run.setPhase(Phase.COMBAT);
        startCombat(run);
        return run;
    }

    private List<String> starterDeck() {
        List<String> deck = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            deck.add(SpireCard.STRIKE.name());
        }
        for (int i = 0; i < 4; i++) {
            deck.add(SpireCard.DEFEND.name());
        }
        deck.add(SpireCard.BASH.name());
        return deck;
    }

    // ------------------------------------------------------------------
    // Combat setup
    // ------------------------------------------------------------------

    public void startCombat(SpireRun run) {
        CombatState s = new CombatState();
        s.setSeed(run.getSeed());
        s.setTurnNumber(0);
        s.setEnemies(buildEnemies(run));

        List<SpireCard> draw = new ArrayList<>();
        for (String id : run.getMasterDeck()) {
            draw.add(SpireCard.valueOf(id));
        }
        Collections.shuffle(draw, rng(run, Decision.COMBAT_SHUFFLE));
        s.setDrawPile(draw);

        for (EnemyInstance e : s.getEnemies()) {
            initIntent(run, s, e);
        }
        run.setCombatState(s);
        startPlayerTurn(run, s);
    }

    private List<EnemyInstance> buildEnemies(SpireRun run) {
        List<EnemyInstance> enemies = new ArrayList<>();
        if (Floor.forNumber(run.getCurrentFloor()) == Floor.BOSS) {
            enemies.add(new EnemyInstance(SpireEnemy.TADPOLE_THE_TERRIBLE));
            return enemies;
        }
        Random rng = rng(run, Decision.ENEMY_COMP);
        List<SpireEnemy> easyPool = Arrays.stream(SpireEnemy.values())
                .filter(e -> e.getTier() == SpireEnemy.Tier.EASY)
                .toList();
        List<SpireEnemy> hardPool = Arrays.stream(SpireEnemy.values())
                .filter(e -> e.getTier() == SpireEnemy.Tier.HARD)
                .toList();
        int easyCount = 1;
        int hardCount = 0;
        switch (run.getCurrentFloor()) {
            case 1:
                easyCount = 1;
                hardCount = 0;
                break;
            case 2:
                easyCount = 1 + rng.nextInt(1);
                hardCount = 0;
                break;
            case 3:
                easyCount = 2 + rng.nextInt(1);
                hardCount = 0;
                break;
            case 4:
                easyCount = 0;
                hardCount = 0;
                break;
            case 5:
                easyCount = 0;
                hardCount = 2;
                break;
            case 6:
                easyCount = 2;
                hardCount = 1;
                break;
            case 7:
                easyCount = 4 + rng.nextInt(1);
                hardCount = 0;
                break;
        }
        for (int i = 0; i < easyCount; i++) {
            enemies.add(new EnemyInstance(easyPool.get(rng.nextInt(easyPool.size()))));
        }
        for (int i = 0; i < hardCount; i++) {
            enemies.add(new EnemyInstance(hardPool.get(rng.nextInt(hardPool.size()))));
        }
        return enemies;
    }

    private void initIntent(SpireRun run, CombatState s, EnemyInstance e) {
        if (e.getEnemyType().isBoss()) {
            e.setCurrentIntent(SpireEnemy.bossIntent(null, false, bossRng(run, s)));
        } else {
            e.setIntentIndex(0);
            e.setCurrentIntent(copy(e.getEnemyType().getScript().get(0)));
        }
    }

    // ------------------------------------------------------------------
    // Turn loop
    // ------------------------------------------------------------------

    private void startPlayerTurn(SpireRun run, CombatState s) {
        s.setTurnNumber(s.getTurnNumber() + 1);
        s.getPlayerEffects().put(StatusEffect.BLOCK, 0);
        s.setEnergy(ENERGY_PER_TURN);
        for (SpireCard power : s.getActivePowers()) {
            if (power.getOnTurnStart() != null) {
                power.getOnTurnStart().apply(this, s);
            }
        }
        drawCards(s, CARDS_PER_TURN);
    }

    /** Plays the hand card at {@code handIndex}. Assumes the caller verified affordability. */
    public void playCard(CombatState s, int handIndex) {
        SpireCard card = s.getHand().remove(handIndex);
        s.setEnergy(s.getEnergy() - card.getCost());
        card.getOnPlay().apply(this, s);
        if (card.getType() == SpireCard.CardType.POWER) {
            s.getActivePowers().add(card);
        } else {
            s.getDiscardPile().add(card);
        }
    }

    /** Ends the player's turn: discard the hand, run the enemy turn, then start the next player turn. */
    public void endPlayerTurn(SpireRun run, CombatState s) {
        s.getDiscardPile().addAll(s.getHand());
        s.getHand().clear();
        resolveEnemyTurn(run, s);
        if (run.getPlayerHp() > 0 && !s.getEnemies().isEmpty()) {
            startPlayerTurn(run, s);
        }
    }

    private void resolveEnemyTurn(SpireRun run, CombatState s) {
        List<String> log = new ArrayList<>();
        for (EnemyInstance e : s.getEnemies()) {
            e.getEffects().put(StatusEffect.BLOCK, 0);
        }
        // Snapshot the player's debuffs before the enemy turn: only stacks that
        // pre-date the turn tick down afterwards, so a Vulnerable/Weak an enemy
        // lands this turn keeps its full value into the player's next turn.
        int preVulnerable = statusOf(s.getPlayerEffects(), StatusEffect.VULNERABLE);
        int preWeak = statusOf(s.getPlayerEffects(), StatusEffect.WEAK);
        for (EnemyInstance e : new ArrayList<>(s.getEnemies())) {
            if (run.getPlayerHp() <= 0) {
                break;
            }
            if (e.isAlive()) {
                resolveIntent(run, s, e, log);
            }
        }
        s.setLastTurnLog(log);
        if (run.getPlayerHp() > 0) {
            for (EnemyInstance e : s.getEnemies()) {
                decay(e.getEffects());
                advanceIntent(run, s, e);
            }
            decayPlayerDebuffs(s, preVulnerable, preWeak);
        }
    }

    private void resolveIntent(SpireRun run, CombatState s, EnemyInstance e, List<String> log) {
        Intent intent = e.getCurrentIntent();
        String name = e.getEnemyType().getDisplayName();
        switch (intent.getType()) {
            case ATTACK, NUKE -> {
                int raw = computeAttackDamage(intent.getValue(), e.getEffects(), s.getPlayerEffects());
                int absorbed = absorbWithBlock(s.getPlayerEffects(), raw);
                int toHp = raw - absorbed;
                run.setPlayerHp(Math.max(0, run.getPlayerHp() - toHp));
                if (absorbed > 0) {
                    log.add(String.format("%s hit you for %d (%d absorbed by block)", name, toHp, absorbed));
                } else {
                    log.add(String.format("%s hit you for %d", name, toHp));
                }
            }
            case BLOCK -> {
                addEffect(e.getEffects(), StatusEffect.BLOCK, intent.getValue());
                log.add(String.format("%s gained %d Block", name, intent.getValue()));
            }
            case BUFF_STRENGTH -> {
                addEffect(e.getEffects(), StatusEffect.STRENGTH, intent.getValue());
                log.add(String.format("%s gained %d Strength", name, intent.getValue()));
            }
            case APPLY_VULNERABLE -> {
                addEffect(s.getPlayerEffects(), StatusEffect.VULNERABLE, intent.getValue());
                log.add(String.format("%s applied %d Vulnerable", name, intent.getValue()));
            }
            case APPLY_WEAK -> {
                addEffect(s.getPlayerEffects(), StatusEffect.WEAK, intent.getValue());
                log.add(String.format("%s applied %d Weak", name, intent.getValue()));
            }
            case HEAL -> {
                int healed = Math.min(intent.getValue(), e.getMaxHp() - e.getCurrentHp());
                e.setCurrentHp(e.getCurrentHp() + healed);
                log.add(String.format("%s healed %d", name, healed));
            }
            case WINDUP -> log.add(String.format("%s is charging up...", name));
        }
    }

    private void advanceIntent(SpireRun run, CombatState s, EnemyInstance e) {
        if (e.getEnemyType().isBoss()) {
            boolean phase2 = e.getCurrentHp() <= SpireEnemy.BOSS_ENRAGE_HP;
            e.setCurrentIntent(SpireEnemy.bossIntent(e.getCurrentIntent(), phase2, bossRng(run, s)));
        } else {
            List<Intent> script = e.getEnemyType().getScript();
            int next = (e.getIntentIndex() + 1) % script.size();
            e.setIntentIndex(next);
            e.setCurrentIntent(copy(script.get(next)));
        }
    }

    public boolean isCombatWon(SpireRun run) {
        return run.getCombatState() != null && run.getCombatState().getEnemies().isEmpty();
    }

    public boolean isBossFloor(SpireRun run) {
        return Floor.forNumber(run.getCurrentFloor()) == Floor.BOSS;
    }

    // ------------------------------------------------------------------
    // Phase transitions
    // ------------------------------------------------------------------

    public void toCardReward(SpireRun run) {
        run.setCombatState(null);
        run.setPhase(Phase.CARD_REWARD);
        run.setCurrentRewardOffer(generateRewardOffer(run));
    }

    private List<String> generateRewardOffer(SpireRun run) {
        List<SpireCard> pool = new ArrayList<>(SpireCard.rewardPool());
        Collections.shuffle(pool, rng(run, Decision.REWARD));
        return pool.subList(0, Math.min(REWARD_OFFER_SIZE, pool.size()))
                .stream()
                .map(SpireCard::name)
                .toList();
    }

    public void pickReward(SpireRun run, int offerIndex) {
        run.getMasterDeck().add(run.getCurrentRewardOffer().get(offerIndex));
        advanceFloor(run);
    }

    public void skipReward(SpireRun run) {
        advanceFloor(run);
    }

    private void advanceFloor(SpireRun run) {
        run.setCurrentRewardOffer(null);
        run.setCurrentFloor(run.getCurrentFloor() + 1);
        if (Floor.forNumber(run.getCurrentFloor()) == Floor.REST) {
            run.setPhase(Phase.REST_CHOICE);
        } else {
            run.setPhase(Phase.COMBAT);
            startCombat(run);
        }
    }

    public void restHeal(SpireRun run) {
        int heal = run.getPlayerMaxHp() * REST_HEAL_PERCENT / 100;
        run.setPlayerHp(Math.min(run.getPlayerMaxHp(), run.getPlayerHp() + heal));
        advanceFloor(run);
    }

    public void restRemoveStart(SpireRun run) {
        run.setPhase(Phase.CARD_REMOVAL);
    }

    public void restCancelRemoval(SpireRun run) {
        run.setPhase(Phase.REST_CHOICE);
    }

    public void removeCard(SpireRun run, int deckIndex) {
        run.getMasterDeck().remove(deckIndex);
        advanceFloor(run);
    }

    // ------------------------------------------------------------------
    // Card primitives — called by SpireCard effect lambdas
    // ------------------------------------------------------------------

    public void dealDamageToFront(CombatState s, int base) {
        EnemyInstance front = frontEnemy(s);
        if (front != null) {
            damageEnemy(s, front, base);
        }
    }

    public void dealDamageToBack(CombatState s, int base) {
        if (!s.getEnemies().isEmpty()) {
            damageEnemy(s, s.getEnemies().get(s.getEnemies().size() - 1), base);
        }
    }

    public void dealDamageToAll(CombatState s, int base) {
        for (EnemyInstance e : new ArrayList<>(s.getEnemies())) {
            damageEnemy(s, e, base);
        }
    }

    private void damageEnemy(CombatState s, EnemyInstance e, int base) {
        int raw = computeAttackDamage(base, s.getPlayerEffects(), e.getEffects());
        int absorbed = absorbWithBlock(e.getEffects(), raw);
        e.setCurrentHp(e.getCurrentHp() - (raw - absorbed));
        s.getEnemies().removeIf(enemy -> enemy.getCurrentHp() <= 0);
    }

    public void gainBlock(CombatState s, int amount) {
        addEffect(s.getPlayerEffects(), StatusEffect.BLOCK, amount);
    }

    public void doubleBlock(CombatState s) {
        s.getPlayerEffects().put(StatusEffect.BLOCK, statusOf(s.getPlayerEffects(), StatusEffect.BLOCK) * 2);
    }

    public void gainStrength(CombatState s, int amount) {
        addEffect(s.getPlayerEffects(), StatusEffect.STRENGTH, amount);
    }

    public void gainEnergy(CombatState s, int amount) {
        s.setEnergy(s.getEnergy() + amount);
    }

    public void applyVulnerableToFront(CombatState s, int stacks) {
        EnemyInstance front = frontEnemy(s);
        if (front != null) {
            addEffect(front.getEffects(), StatusEffect.VULNERABLE, stacks);
        }
    }

    public void applyWeakToAll(CombatState s, int stacks) {
        for (EnemyInstance e : s.getEnemies()) {
            addEffect(e.getEffects(), StatusEffect.WEAK, stacks);
        }
    }

    public void enemyFrontLoseStrength(CombatState s, int amount) {
        EnemyInstance front = frontEnemy(s);
        if (front != null) {
            addEffect(front.getEffects(), StatusEffect.STRENGTH, -amount);
        }
    }

    public void drawCards(CombatState s, int count) {
        for (int i = 0; i < count; i++) {
            if (s.getDrawPile().isEmpty()) {
                if (s.getDiscardPile().isEmpty()) {
                    return;
                }
                s.getDrawPile().addAll(s.getDiscardPile());
                s.getDiscardPile().clear();
                Collections.shuffle(s.getDrawPile(), reshuffleRng(s));
            }
            SpireCard card = s.getDrawPile().remove(0);
            if (s.getHand().size() >= MAX_HAND_SIZE) {
                s.getDiscardPile().add(card);
            } else {
                s.getHand().add(card);
            }
        }
    }

    /** Dropkick: 8 damage to front; bonus energy + draw if the front enemy is Vulnerable. */
    public void dropkick(CombatState s) {
        EnemyInstance front = frontEnemy(s);
        boolean vulnerable = front != null && statusOf(front.getEffects(), StatusEffect.VULNERABLE) > 0;
        dealDamageToFront(s, 8);
        if (vulnerable) {
            gainEnergy(s, 1);
            drawCards(s, 1);
        }
    }

    // ------------------------------------------------------------------
    // Combat math helpers
    // ------------------------------------------------------------------

    public int computeAttackDamage(int base, Map<StatusEffect, Integer> attacker, Map<StatusEffect, Integer> target) {
        int dmg = base + statusOf(attacker, StatusEffect.STRENGTH);
        if (statusOf(attacker, StatusEffect.WEAK) > 0) {
            dmg = (int) Math.floor(dmg * 0.75);
        }
        if (statusOf(target, StatusEffect.VULNERABLE) > 0) {
            dmg = (int) Math.floor(dmg * 1.5);
        }
        return Math.max(0, dmg);
    }

    private int absorbWithBlock(Map<StatusEffect, Integer> effects, int incoming) {
        int block = statusOf(effects, StatusEffect.BLOCK);
        int absorbed = Math.min(block, incoming);
        effects.put(StatusEffect.BLOCK, block - absorbed);
        return absorbed;
    }

    private void decay(Map<StatusEffect, Integer> effects) {
        for (StatusEffect se : List.of(StatusEffect.VULNERABLE, StatusEffect.WEAK)) {
            int v = statusOf(effects, se);
            if (v > 0) {
                effects.put(se, v - 1);
            }
        }
    }

    /**
     * Ticks the player's Vulnerable/Weak down by one stack each at the end of the
     * enemy turn — but only stacks that pre-dated the turn ({@code preVulnerable} /
     * {@code preWeak}), so a debuff an enemy lands this turn isn't spent before it
     * has had a turn to bite. Vulnerable N thus boosts a full N enemy turns,
     * mirroring how enemy-held Vulnerable behaves.
     */
    private void decayPlayerDebuffs(CombatState s, int preVulnerable, int preWeak) {
        Map<StatusEffect, Integer> effects = s.getPlayerEffects();
        if (preVulnerable > 0) {
            effects.put(StatusEffect.VULNERABLE, statusOf(effects, StatusEffect.VULNERABLE) - 1);
        }
        if (preWeak > 0) {
            effects.put(StatusEffect.WEAK, statusOf(effects, StatusEffect.WEAK) - 1);
        }
    }

    public int statusOf(Map<StatusEffect, Integer> effects, StatusEffect effect) {
        return effects.getOrDefault(effect, 0);
    }

    private void addEffect(Map<StatusEffect, Integer> effects, StatusEffect effect, int delta) {
        effects.put(effect, statusOf(effects, effect) + delta);
    }

    public EnemyInstance frontEnemy(CombatState s) {
        return s.getEnemies().isEmpty() ? null : s.getEnemies().get(0);
    }

    /** Human-readable label for an enemy's telegraphed intent; attack values include modifiers. */
    public String intentText(CombatState s, EnemyInstance e) {
        Intent intent = e.getCurrentIntent();
        if (intent == null) {
            return "...";
        }
        return switch (intent.getType()) {
            case ATTACK -> "Attack " + computeAttackDamage(intent.getValue(), e.getEffects(), s.getPlayerEffects())
                    + attackModifierTags(e, s);
            case NUKE -> "NUKE " + computeAttackDamage(intent.getValue(), e.getEffects(), s.getPlayerEffects())
                    + attackModifierTags(e, s);
            case BLOCK -> "Block " + intent.getValue();
            case BUFF_STRENGTH -> "Buff +" + intent.getValue() + " Strength";
            case APPLY_VULNERABLE -> "Apply Vulnerable " + intent.getValue();
            case APPLY_WEAK -> "Apply Weak " + intent.getValue();
            case HEAL -> "Heal " + intent.getValue();
            case WINDUP -> "Charging up...";
        };
    }

    /**
     * Parenthetical naming the modifiers folded into a displayed attack number —
     * the attacker's Weak and the player's Vulnerable — so a reduced or boosted
     * value is visibly so. Empty when the attack is unmodified.
     */
    private String attackModifierTags(EnemyInstance attacker, CombatState s) {
        List<String> tags = new ArrayList<>();
        if (statusOf(attacker.getEffects(), StatusEffect.WEAK) > 0) {
            tags.add("weak");
        }
        if (statusOf(s.getPlayerEffects(), StatusEffect.VULNERABLE) > 0) {
            tags.add("vuln");
        }
        return tags.isEmpty() ? "" : " (" + String.join(", ", tags) + ")";
    }

    // ------------------------------------------------------------------
    // RNG
    // ------------------------------------------------------------------

    private static Intent copy(Intent intent) {
        return Intent.of(intent.getType(), intent.getValue());
    }

    private static Random rng(SpireRun run, Decision decision) {
        return new Random(run.getSeed() ^ ((long) run.getCurrentFloor() << 20) ^ ((long) decision.ordinal() << 8));
    }

    private static Random bossRng(SpireRun run, CombatState s) {
        long mix = (long) s.getTurnNumber() * 0x9E3779B97F4A7C15L;
        return new Random(run.getSeed() ^ ((long) run.getCurrentFloor() << 20)
                ^ ((long) Decision.BOSS_INTENT.ordinal() << 8) ^ mix);
    }

    private static Random reshuffleRng(CombatState s) {
        long mix = (long) s.getTurnNumber() * 0x9E3779B97F4A7C15L;
        return new Random(s.getSeed() ^ mix ^ s.getDrawPile().size());
    }
}
