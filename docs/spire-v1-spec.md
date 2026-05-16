# Spire v1 — Design Spec

A mini Slay-the-Spire-like roguelike deckbuilder, played via `/spire` in Discord.

## Scope

In:
- Card-play combat with energy/hand/turn loop
- Deck-building between fights (card rewards, card removal)
- A linear 8-floor run with one boss
- One character (no class selection)
- Status effects: Block, Vulnerable, Weak, Strength
- Status effects apply to both player and enemies

Out (deferred to v2+):
- Relics
- Potions
- Shops
- Random events
- Card upgrades (no `+` versions)
- Branching maps
- Multiple characters
- Ascension difficulty levels
- Poison and other status effects beyond the v1 four
- Elite encounters

## Combat mechanics

| Field | Value |
|---|---|
| Player starting HP | 80 |
| Energy per turn | 6 |
| Cards drawn per turn | 4 |
| Max hand size | 8 |
| Hand overflow rule | Excess cards drawn beyond max hand size go to discard pile |
| End-of-turn rule | Full hand discard |
| Empty draw pile | Auto-reshuffle discard into draw, silently |
| Both piles empty | Draw fewer cards, no error |
| Block decay | Block resets to 0 at start of player's turn |
| Player healing | None between fights; healing only via the one rest site per run |

### Turn structure

1. Start of player turn: refill energy to 6, draw 4 cards (overflow to discard if over max 8), reset block to 0, decay status effects that tick per turn.
2. Player plays cards in any order, subject to energy cost. Unaffordable cards are rendered as disabled buttons.
3. Player ends turn. Hand fully discards.
4. Enemies act front-to-back in positional order, resolving their pre-displayed intents.
5. End of enemy turn: each enemy computes its next-turn intent (visible to player before player's next turn).
6. Repeat from step 1.

### Targeting model

- Up to 5 enemy slots per combat.
- Enemy position is determined by list order. Position 1 is the front.
- When an enemy dies, all enemies behind it shift forward by one position. Position 1 is always the most-forward living enemy.
- Cards encode their own targeting (e.g., "Strike Front", "Cleave All", "Snipe Back"). The player never picks a target — single-click to play a card.
- Bosses are 1-enemy fights, so targeting cards still work but trivially hit the only enemy.

### Multi-enemy resolution

- All enemies act in front-to-back order on enemy turn.
- All actions resolve server-side in a single tick; the UI displays the resulting state with a "last turn" log block.
- If the player drops to <=0 HP mid-enemy-sequence, combat ends immediately and remaining enemy actions are skipped.
- If the last enemy dies mid-card-resolution during the player turn, the card finishes resolving and then combat ends. Remaining hand/energy are discarded with the combat state.

## Status effects (v1)

| Name | Type | Behavior |
|---|---|---|
| Block | Resource | Absorbs incoming attack damage. Resets to 0 at start of holder's turn. |
| Vulnerable | Debuff | Holder takes +50% damage from attacks. N stacks last N turns of being attacked; a stack applied mid-turn isn't spent until the next turn. |
| Weak | Debuff | Holder deals -25% damage with attacks. N stacks last N of the holder's attacking turns; a stack applied mid-turn isn't spent until the next turn. |
| Strength | Buff | +1 damage per stack on attacks the holder deals. Persistent (no decay). |

Status effects exist on both players and enemies using the same model.

## Cards

- Library size: ~24 distinct cards for v1.
  - ~10 attacks (Strike, Heavy Strike, Cleave, Bash, Pommel-Strike-style with draw, etc.)
  - ~8 skills (Defend, Heavy Defend, Shrug-It-Off-style, etc.)
  - ~4 powers (persistent passives like Inflame's strength buff, Demon-Form-style scaling)
  - 2 starter overlaps (Strike, Defend, Bash already in the starter deck)
- Representation: hardcoded Java enum, each value implementing an `apply(CombatState, ...)` method. No data-driven DSL.
- Card cost range: 0-6 energy (doubled from STS's 0-3 to allow more cost granularity).

### Starting deck (10 cards)

| Count | Card | Cost | Effect |
|---|---|---|---|
| 5 | Strike | 2 | Deal 6 damage to front |
| 4 | Defend | 2 | Gain 5 Block |
| 1 | Bash | 4 | Deal 8 damage to front, apply 2 Vulnerable to front |

### Deck-building loop

- After every regular combat (floors 1-3 and 5-7), the player gets a 1-of-3 random card pick from the card pool, with the option to skip.
- After the boss (floor 8), no card reward — run is complete.
- The rest site at floor 4 lets the player either heal 30% of max HP or remove one card from their master deck.
- No card upgrades in v1.

### Deck management between fights

- The `SpireRun` stores the **master deck** as a `List<String>` of card IDs.
- When a combat starts: draw pile = shuffled copy of master deck; hand = empty; discard pile = empty.
- During combat, cards move between draw pile / hand / discard pile only.
- When combat ends: combat-scoped piles are discarded. The master deck is unchanged.
- The master deck is mutated only by card rewards (additions) and rest-site removals.

## Run structure

8 floors, linear, randomized content:

| Floor | Type | Notes |
|---|---|---|
| 1 | Combat | Easy tier (1-2 enemies, low HP) |
| 2 | Combat | Easy tier |
| 3 | Combat | Easy tier |
| 4 | Rest site | Heal 30% HP OR remove a card |
| 5 | Combat | Hard tier (2-4 enemies, scaling HP) |
| 6 | Combat | Hard tier |
| 7 | Combat | Hard tier |
| 8 | Boss | Single phase-shift boss |

- Enemy composition for each combat floor is drawn randomly from the tier pool.
- Card-reward offerings are drawn randomly from the card pool.
- Rest site is fixed at floor 4.
- Randomization is seeded per run (see Persistence).

## Enemies

7 enemy types total: 6 regular + 1 boss.

- 3 easy-tier enemies for floors 1-3 (low HP, simple intent rotation; e.g., ramping-strength cultist, hit-then-block worm, splitting slime).
- 3 hard-tier enemies for floors 5-7 (higher HP, more dangerous intent; e.g., debuff-applier, telegraphed nuke).
- 1 boss for floor 8 with ~80 HP and a phase-shifted intent rotation.

### Intent model

- Regular enemies use **deterministic scripts**: an ordered list of intents that cycles. E.g., `[Attack 6, Block 5, Apply Vulnerable 2]` repeats.
- The boss uses **weighted random with constraints** across two phases.
  - Pre-50%-HP: one rotation.
  - At/below 50% HP: switches to an enraged rotation. Includes one telegraphed "wind-up" intent where the boss does no action this turn but displays a massive next-turn nuke (e.g., "Charging Up - Inferno 40 dmg to all next turn"). Forces the player to dump block.
- Next-turn intent is computed at the end of each enemy's turn and stored on the enemy instance so the player sees it during their turn.

### Representation

- `SpireEnemy` enum: definitions and intent logic.
- `EnemyInstance` (embedded in `CombatState`): per-combat live data (current HP, status effects, intent-rotation index, current intent).

## Persistence model

### One active run per user

- `SpireRun` document with a unique index on `discordId`.
- `/spire` either opens the user's current run or starts a new one.
- The user can only have one run at a time.

### Run document fields (high-level)

- `discordId` (unique-indexed)
- `seed` (long) - per-run RNG seed
- `currentFloor` (int, 1-8)
- `phase` (enum: COMBAT, CARD_REWARD, REST_CHOICE, CARD_REMOVAL)
- `playerHp`, `playerMaxHp` (int)
- `masterDeck` (`List<String>` of card IDs)
- `combatState` (embedded, present only when phase = COMBAT) - hand, draw pile, discard pile, energy, player status effects, enemies (each with HP/statuses/intent)
- `currentRewardOffer` (`List<String>` of card IDs, present when phase = CARD_REWARD)
- `currentMessageId` (string) - the Discord message ID of the active game message
- `currentChannelId` (string) - the Discord channel ID where the active message lives
- `createdAt`, `updatedAt`

### Death and victory

- On player death OR boss kill: the run document is deleted immediately after the final summary message is posted.
- The final message is informational only - no "Play Again" button. Players run `/spire` again to start fresh.

### Abandonment

- `/spire` on an existing run shows a "Continue or Abandon" pair of buttons (instead of jumping straight into the current game state). Abandoning deletes the run; continuing re-renders the current phase.

### Idle timeout

- None. Runs can sit indefinitely.

### Bot restart resilience

- All state is serialized to Mongo on every action. A bot restart mid-fight preserves the exact state: hand contents, draw pile order (per-seed deterministic), enemy HP, intent, status stacks, etc.

### RNG / seeding

- Per-run seed stored on the document.
- Sub-seeds derived per-decision as `Random(seed XOR floor XOR decisionType.ordinal())` so randomization is stable across restarts at the same decision point.
- This is groundwork for a future "daily seed" feature (everyone gets the same run today).

## UI / message model

### Visibility — public + delete-and-repost

- Game messages are **public** so friends in the channel can spectate.
- On every state change, the bot **deletes the previous game message and posts a new one** so the game always sits at the bottom of the channel, below recent chat.

Implementation pattern:
- `/spire` first time: `event.reply(content).queue()`. Persist the resulting message ID to `currentMessageId` / `currentChannelId`.
- Subsequent button interactions:
  1. `event.deferReply().queue()` to acknowledge within 3s.
  2. `event.getMessage().delete().queue()` to remove the old game message.
  3. `event.getHook().sendMessage(newContent).queue()` to post fresh.
  4. Persist the new message ID.
- Modal submissions: same delete-and-repost pattern, except the modal handler reads `currentMessageId` from the run document and resolves the message via the channel API.
- Failures of `delete()` (message manually deleted, channel removed) are handled with an error callback that ignores the failure and posts fresh anyway.

### Button gating

- Every button handler starts with:
  ```java
  if (!event.getUser().getId().equals(run.getDiscordId())) {
      event.reply("This isn't your run.").setEphemeral(true).queue();
      return;
  }
  ```
- Spectators see the messages but cannot interact.

### Cross-channel `/spire`

- A `/spire` in a new channel deletes the old message in the old channel and posts the new message in the new channel. The run "follows" the user.

### Card-play interaction

- Cards in hand are numbered 1..N. Buttons are labeled `[1]` `[2]` ... `[N]`. Card name, cost, and effect text are rendered in the message body.
- Cards the player cannot afford this turn are rendered as disabled buttons (`Button.asDisabled()`).
- Targeting is implicit (positional, encoded by the card). One click per card play.

### Combat message layout

Enemies are listed back-to-front: the front enemy (list position 1) renders last, directly above "You".

```
=== Floor 5 - Combat ===
Slaver   HP 12/20  -> Apply Vulnerable 2
Cultist  HP 22/40  -> Attack 8

You  HP 53/80  [Block 0] [Vuln 2]
Energy 6/6

Last turn:
  - Cultist hit you for 6 (3 absorbed by block)
  - Slaver applied 2 Weak
  - Slime healed 5

Hand:
  1. Strike (2) - 6 dmg to front
  2. Strike (2) - 6 dmg to front
  3. Defend (2) - Gain 5 Block
  4. Cleave (4) - 8 dmg to all
```

Buttons:
- Row 1: card buttons `[1]` `[2]` ... up to the hand size (max 8, splits across two rows at >5).
- Bottom row: `[End Turn]` `[View Deck]` `[View Discard]` `[View Draw]`.

### Card reward layout

```
Victory! Floor 3 cleared. Pick a card:
  1. Cleave (4) - Deal 8 damage to all enemies
  2. Inflame (2, Power) - Gain 2 Strength permanently
  3. Heavy Strike (3) - Deal 14 damage to front
```

Buttons: `[1]` `[2]` `[3]` `[Skip]`

### Rest site layout

```
Rest Site (Floor 4). Choose:
```

Buttons: `[Heal 30%]` `[Remove a card]`

### Card removal layout (paginated)

```
Remove which card?

Page 1/3 - cards 1-5 of 10:
  1. Strike (2) - 6 dmg to front
  2. Strike (2) - 6 dmg to front
  3. Strike (2) - 6 dmg to front
  4. Strike (2) - 6 dmg to front
  5. Strike (2) - 6 dmg to front
```

Buttons: Row 1: `[1] [2] [3] [4] [5]` (selection); Row 2: `[Prev]` `[Next]` `[Cancel]`.

### View deck / discard / draw

- Each `View X` button posts an **ephemeral** text dump listing the card IDs in that pile (sorted by name). Does not modify the public game message.

### Victory / death screens

- Boss defeated: public message reads "Boss defeated! +50 points awarded. Run complete. Use `/spire` to start another." Run document is deleted.
- Player died: public message reads "You died on floor N. Run over. Use `/spire` to try again." Run document is deleted.

## Economy

- Free to start. No point cost to play `/spire`.
- +50 points to the user's `IBUser.points` on boss kill. No partial rewards for clearing floors.
- Abandoning has no effect on previously awarded points (none have been awarded since the only reward is the boss kill).
- Players are free to farm the v1 boss for points. When v2 introduces further bosses, the reward structure can expand without altering this.

## Package structure (mirrors `command/blackjack/`)

```
command/
  Spire.java                              (slash command + button handler + modal handler)
  spire/
    SpireRun.java                         (@Document - root run state)
    CombatState.java                      (embedded - hand/draw/discard/enemies/player effects)
    EnemyInstance.java                    (embedded - one enemy's live combat state)
    SpireCard.java                        (enum - card definitions + apply logic)
    SpireEnemy.java                       (enum - enemy definitions + intent logic)
    StatusEffect.java                     (enum - Block/Vulnerable/Weak/Strength)
    Floor.java                            (enum - COMBAT, REST, BOSS, plus floor->type mapping)
    Phase.java                            (enum - COMBAT, CARD_REWARD, REST_CHOICE, CARD_REMOVAL)
repository/
  SpireRunRepository.java                 (extends MongoRepository<SpireRun, String>, derived findByDiscordId)
service/
  SpireService.java                       (engine: shuffle, draw, play card, enemy turn, settle combat, advance floor)
```

`Spire` itself owns the JDA-facing surface: implements `SlashCommand`, `ButtonHandler`, `ModalHandler` (the same triple as `Blackjack`). It dispatches to `SpireService` for game logic and to render helpers for message construction.

## Phase state machine

```
        +-------- COMBAT --------+
        |                        |
        | (all enemies dead)     | (player dies)
        v                        v
   CARD_REWARD               run deleted
        |
        | (pick or skip)
        v
  floor++ ----- if floor == 4 ----> REST_CHOICE
        |                                |
        | else                           +--- [Heal 30%] -> COMBAT (floor 5)
        v                                |
       COMBAT (next floor)               +--- [Remove a card] -> CARD_REMOVAL
                                                 |
                                                 +--- (pick a card) -> COMBAT (floor 5)
                                                 +--- (cancel) -> REST_CHOICE
```

Boss floor 8: COMBAT ends on boss death -> award +50 points -> delete run -> show victory message.

## Implementation notes

- Persist `SpireRun` after every state-changing action. Same pattern as `BlackjackGame` saving.
- Card effects compose: a single card's `apply` may do damage, apply statuses, draw cards, gain block, etc. The enum value owns its full effect.
- Status effect ticking and end-of-turn cleanup live in `SpireService`, not on individual cards.
- Front-to-back enemy resolution loops on a snapshot of the enemy list to avoid concurrent-modification issues when an enemy dies mid-loop.
- `Floor` enum or a static method maps floor number to encounter type so floor 4's "rest" is encoded in one place.
- Card rewards present 3 distinct cards (no duplicates within an offer).
- All randomized decisions derive `Random` instances from the run seed + floor + decision-type to keep restart-resilience.

## Deferred to v2

- Card upgrades (`+` versions, smithing at rest sites)
- Relics
- Potions
- Shops and events
- Elite encounters
- Branching maps
- Multiple characters
- Daily seed feature (groundwork is in v1 via per-run seed)
- Run history / lifetime stats / leaderboards
- Public-spectator gating refinements (e.g., spectator reactions, watch list)
- Additional status effects: Poison, Dexterity, Frail, Regen, Thorns, Intangible, etc.
- Additional bosses
- "Play again" button on death/victory screens (if it becomes desirable after testing)
