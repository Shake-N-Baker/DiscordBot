package com.ianscottbaker.ianbot.command;

import com.ianscottbaker.ianbot.command.spire.CombatState;
import com.ianscottbaker.ianbot.command.spire.EnemyInstance;
import com.ianscottbaker.ianbot.command.spire.Phase;
import com.ianscottbaker.ianbot.command.spire.SpireCard;
import com.ianscottbaker.ianbot.command.spire.SpireRun;
import com.ianscottbaker.ianbot.command.spire.StatusEffect;
import com.ianscottbaker.ianbot.model.IBUser;
import com.ianscottbaker.ianbot.repository.SpireRunRepository;
import com.ianscottbaker.ianbot.service.SpireService;
import com.ianscottbaker.ianbot.service.UserService;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The {@code /spire} feature: a mini Slay-the-Spire-like roguelike. Owns the
 * JDA-facing surface (slash command + buttons) and message rendering; all game
 * logic lives in {@link SpireService}.
 *
 * <p>Game messages are public so friends can spectate. On every state change the
 * old message is deleted and a fresh one posted, keeping the game at the bottom
 * of the channel. Only the run owner may press buttons.
 */
@Service
public class Spire implements SlashCommand, ButtonHandler {
    public static final String NAME = "spire";
    public static final String PREFIX = "spire";

    private static final String ACTION_PLAY = "PLAY";
    private static final String ACTION_ENDTURN = "ENDTURN";
    private static final String ACTION_VIEW = "VIEW";
    private static final String ACTION_REWARD = "REWARD";
    private static final String ACTION_REST = "REST";
    private static final String ACTION_REMOVE = "REMOVE";
    private static final String ACTION_REMPAGE = "REMPAGE";
    private static final String ACTION_REMCANCEL = "REMCANCEL";
    private static final String ACTION_CONTINUE = "CONTINUE";
    private static final String ACTION_ABANDON = "ABANDON";

    private static final String VIEW_DECK = "DECK";
    private static final String VIEW_DISCARD = "DISCARD";
    private static final String VIEW_DRAW = "DRAW";
    private static final String REWARD_SKIP = "SKIP";
    private static final String REST_HEAL = "HEAL";
    private static final String REST_REMOVE = "REMOVE";

    private static final int REMOVAL_PAGE_SIZE = 5;

    private final SpireRunRepository runRepository;
    private final SpireService spireService;
    private final UserService userService;

    public Spire(SpireRunRepository runRepository, SpireService spireService, UserService userService) {
        this.runRepository = runRepository;
        this.spireService = spireService;
        this.userService = userService;
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash(NAME, "play Spire — a mini Slay-the-Spire-like deckbuilding run");
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    // ------------------------------------------------------------------
    // Slash command
    // ------------------------------------------------------------------

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        runRepository.findByDiscordId(discordId).ifPresentOrElse(
                run -> event.reply(renderContinuePrompt(run)).queue(),
                () -> startFreshRun(event, discordId)
        );
    }

    private void startFreshRun(SlashCommandInteractionEvent event, String discordId) {
        SpireRun run = spireService.newRun(discordId);
        save(run);
        event.reply(renderPhase(run)).queue(hook -> hook.retrieveOriginal().queue(msg -> {
            run.setCurrentMessageId(msg.getId());
            run.setCurrentChannelId(msg.getChannelId());
            save(run);
        }));
    }

    // ------------------------------------------------------------------
    // Button dispatch
    // ------------------------------------------------------------------

    @Override
    public void handle(ButtonInteractionEvent event, String payload) {
        String[] parts = payload.split(ButtonHandler.DELIMITER, 3);
        if (parts.length < 2) {
            event.reply("Unrecognised Spire button.").setEphemeral(true).queue();
            return;
        }
        String action = parts[0];
        String owner = parts[1];
        String arg = parts.length > 2 ? parts[2] : "";

        if (!event.getUser().getId().equals(owner)) {
            event.reply("This isn't your run.").setEphemeral(true).queue();
            return;
        }
        SpireRun run = runRepository.findByDiscordId(owner).orElse(null);
        if (run == null) {
            event.reply("This run is no longer active. Use /spire to start a new one.").setEphemeral(true).queue();
            return;
        }

        switch (action) {
            case ACTION_PLAY -> handlePlay(event, run, arg);
            case ACTION_ENDTURN -> handleEndTurn(event, run);
            case ACTION_VIEW -> handleView(event, run, arg);
            case ACTION_REWARD -> handleReward(event, run, arg);
            case ACTION_REST -> handleRest(event, run, arg);
            case ACTION_REMOVE -> handleRemove(event, run, arg);
            case ACTION_REMPAGE -> handleRemovalPage(event, run, arg);
            case ACTION_REMCANCEL -> handleRemovalCancel(event, run);
            case ACTION_CONTINUE -> repost(event, run, renderPhase(run));
            case ACTION_ABANDON -> handleAbandon(event, run);
            default -> event.reply("uncaught spire action " + action).setEphemeral(true).queue();
        }
    }

    // ------------------------------------------------------------------
    // Combat actions
    // ------------------------------------------------------------------

    private void handlePlay(ButtonInteractionEvent event, SpireRun run, String arg) {
        if (run.getPhase() != Phase.COMBAT || run.getCombatState() == null) {
            event.reply("You're not in combat right now.").setEphemeral(true).queue();
            return;
        }
        CombatState s = run.getCombatState();
        Integer index = parseIndex(arg);
        if (index == null || index < 0 || index >= s.getHand().size()) {
            event.reply("That card is no longer in your hand.").setEphemeral(true).queue();
            return;
        }
        SpireCard card = s.getHand().get(index);
        if (card.getCost() > s.getEnergy()) {
            event.reply("Not enough energy to play that card.").setEphemeral(true).queue();
            return;
        }

        spireService.playCard(s, index);
        if (spireService.isCombatWon(run)) {
            onCombatWon(event, run);
            return;
        }
        save(run);
        repost(event, run, renderCombat(run));
    }

    private void onCombatWon(ButtonInteractionEvent event, SpireRun run) {
        if (spireService.isBossFloor(run)) {
            IBUser user = userService.getOrCreate(run.getDiscordId());
            user.setPoints(user.getPoints() + SpireService.BOSS_REWARD_POINTS);
            userService.save(user);
            runRepository.deleteByDiscordId(run.getDiscordId());
            postFinal(event, run, plain(String.format(
                    "Boss defeated! +%d points awarded. Run complete. Use /spire to start another.",
                    SpireService.BOSS_REWARD_POINTS)));
            return;
        }
        spireService.toCardReward(run);
        save(run);
        repost(event, run, renderCardReward(run));
    }

    private void handleEndTurn(ButtonInteractionEvent event, SpireRun run) {
        if (run.getPhase() != Phase.COMBAT || run.getCombatState() == null) {
            event.reply("You're not in combat right now.").setEphemeral(true).queue();
            return;
        }
        int floor = run.getCurrentFloor();
        spireService.endPlayerTurn(run, run.getCombatState());
        if (run.getPlayerHp() <= 0) {
            runRepository.deleteByDiscordId(run.getDiscordId());
            postFinal(event, run, plain(String.format(
                    "You died on floor %d. Run over. Use /spire to try again.", floor)));
            return;
        }
        save(run);
        repost(event, run, renderCombat(run));
    }

    private void handleView(ButtonInteractionEvent event, SpireRun run, String arg) {
        if (run.getPhase() != Phase.COMBAT || run.getCombatState() == null) {
            event.reply("There's nothing to view right now.").setEphemeral(true).queue();
            return;
        }
        CombatState s = run.getCombatState();
        List<SpireCard> pile = switch (arg) {
            case VIEW_DECK -> run.getMasterDeck().stream().map(SpireCard::valueOf).collect(Collectors.toList());
            case VIEW_DISCARD -> new ArrayList<>(s.getDiscardPile());
            case VIEW_DRAW -> new ArrayList<>(s.getDrawPile());
            default -> new ArrayList<>();
        };
        String title = switch (arg) {
            case VIEW_DECK -> "Master deck";
            case VIEW_DISCARD -> "Discard pile";
            case VIEW_DRAW -> "Draw pile";
            default -> "Cards";
        };
        pile.sort(Comparator.comparing(SpireCard::getDisplayName));
        String body = pile.isEmpty()
                ? "(empty)"
                : pile.stream().map(c -> "  " + cardLabel(c, false)).collect(Collectors.joining("\n"));
        event.reply(String.format("**%s** (%d cards)\n```\n%s\n```", title, pile.size(), body))
                .setEphemeral(true).queue();
    }

    // ------------------------------------------------------------------
    // Reward / rest / removal actions
    // ------------------------------------------------------------------

    private void handleReward(ButtonInteractionEvent event, SpireRun run, String arg) {
        if (run.getPhase() != Phase.CARD_REWARD || run.getCurrentRewardOffer() == null) {
            event.reply("There's no card reward to pick right now.").setEphemeral(true).queue();
            return;
        }
        if (REWARD_SKIP.equals(arg)) {
            spireService.skipReward(run);
        } else {
            Integer index = parseIndex(arg);
            if (index == null || index < 0 || index >= run.getCurrentRewardOffer().size()) {
                event.reply("That card is no longer on offer.").setEphemeral(true).queue();
                return;
            }
            spireService.pickReward(run, index);
        }
        save(run);
        repost(event, run, renderPhase(run));
    }

    private void handleRest(ButtonInteractionEvent event, SpireRun run, String arg) {
        if (run.getPhase() != Phase.REST_CHOICE) {
            event.reply("You're not at a rest site right now.").setEphemeral(true).queue();
            return;
        }
        if (REST_HEAL.equals(arg)) {
            spireService.restHeal(run);
        } else if (REST_REMOVE.equals(arg)) {
            spireService.restRemoveStart(run);
        } else {
            event.reply("Unrecognised rest choice.").setEphemeral(true).queue();
            return;
        }
        save(run);
        repost(event, run, renderPhase(run));
    }

    private void handleRemove(ButtonInteractionEvent event, SpireRun run, String arg) {
        if (run.getPhase() != Phase.CARD_REMOVAL) {
            event.reply("There's no card to remove right now.").setEphemeral(true).queue();
            return;
        }
        Integer index = parseIndex(arg);
        if (index == null || index < 0 || index >= run.getMasterDeck().size()) {
            event.reply("That card is no longer in your deck.").setEphemeral(true).queue();
            return;
        }
        spireService.removeCard(run, index);
        save(run);
        repost(event, run, renderPhase(run));
    }

    private void handleRemovalPage(ButtonInteractionEvent event, SpireRun run, String arg) {
        if (run.getPhase() != Phase.CARD_REMOVAL) {
            event.reply("There's no card to remove right now.").setEphemeral(true).queue();
            return;
        }
        Integer page = parseIndex(arg);
        repost(event, run, renderCardRemoval(run, page == null ? 0 : page));
    }

    private void handleRemovalCancel(ButtonInteractionEvent event, SpireRun run) {
        if (run.getPhase() != Phase.CARD_REMOVAL) {
            event.reply("There's nothing to cancel right now.").setEphemeral(true).queue();
            return;
        }
        spireService.restCancelRemoval(run);
        save(run);
        repost(event, run, renderPhase(run));
    }

    private void handleAbandon(ButtonInteractionEvent event, SpireRun run) {
        runRepository.deleteByDiscordId(run.getDiscordId());
        deleteStoredMessage(event, run);
        event.editMessage("Spire run abandoned. Use /spire to start a new one.")
                .setComponents()
                .queue(ok -> { }, err -> { });
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    private MessageCreateData renderPhase(SpireRun run) {
        return switch (run.getPhase()) {
            case COMBAT -> renderCombat(run);
            case CARD_REWARD -> renderCardReward(run);
            case REST_CHOICE -> renderRest(run);
            case CARD_REMOVAL -> renderCardRemoval(run, 0);
        };
    }

    private MessageCreateData renderCombat(SpireRun run) {
        CombatState s = run.getCombatState();
        String owner = run.getDiscordId();
        StringBuilder body = new StringBuilder();
        body.append(String.format("=== Floor %d/%d - Combat ===%n", run.getCurrentFloor(), 8));

        // Rendered back-to-front: the front enemy (list index 0) prints last, nearest "You".
        List<EnemyInstance> enemies = s.getEnemies();
        for (int i = enemies.size() - 1; i >= 0; i--) {
            EnemyInstance e = enemies.get(i);
            String statuses = enemyStatusLabel(e);
            body.append(String.format("%-12s HP %d/%d%s  -> %s%n",
                    e.getEnemyType().getDisplayName(),
                    e.getCurrentHp(), e.getMaxHp(),
                    statuses.isEmpty() ? "" : "  " + statuses,
                    spireService.intentText(s, e)));
        }

        body.append(String.format("%nYou          HP %d/%d  %s%n",
                run.getPlayerHp(), run.getPlayerMaxHp(), playerStatusLabel(s)));
        body.append(String.format("Energy %d/%d%n", s.getEnergy(), SpireService.ENERGY_PER_TURN));

        if (!s.getLastTurnLog().isEmpty()) {
            body.append(String.format("%nLast turn:%n"));
            for (String line : s.getLastTurnLog()) {
                body.append("  - ").append(line).append(System.lineSeparator());
            }
        }

        body.append(String.format("%nHand:%n"));
        if (s.getHand().isEmpty()) {
            body.append("  (empty)\n");
        } else {
            for (int i = 0; i < s.getHand().size(); i++) {
                body.append(String.format("  %d. %s%n", i + 1, cardLabel(s.getHand().get(i), true)));
            }
        }

        List<Button> cardButtons = new ArrayList<>();
        for (int i = 0; i < s.getHand().size(); i++) {
            SpireCard card = s.getHand().get(i);
            Button button = Button.primary(buttonId(ACTION_PLAY, owner, String.valueOf(i)), "[" + (i + 1) + "]");
            if (card.getCost() > s.getEnergy()) {
                button = button.asDisabled();
            }
            cardButtons.add(button);
        }
        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < cardButtons.size(); i += 5) {
            rows.add(ActionRow.of(cardButtons.subList(i, Math.min(cardButtons.size(), i + 5))));
        }
        rows.add(ActionRow.of(
                Button.danger(buttonId(ACTION_ENDTURN, owner, ""), "End Turn"),
                Button.secondary(buttonId(ACTION_VIEW, owner, VIEW_DECK), "View Deck"),
                Button.secondary(buttonId(ACTION_VIEW, owner, VIEW_DISCARD), "View Discard"),
                Button.secondary(buttonId(ACTION_VIEW, owner, VIEW_DRAW), "View Draw")));

        return new MessageCreateBuilder()
                .addContent(codeBlock(body.toString()))
                .setComponents(rows)
                .build();
    }

    private MessageCreateData renderCardReward(SpireRun run) {
        String owner = run.getDiscordId();
        List<String> offer = run.getCurrentRewardOffer();
        StringBuilder body = new StringBuilder();
        body.append(String.format("Victory! Floor %d cleared. Pick a card:%n", run.getCurrentFloor()));
        for (int i = 0; i < offer.size(); i++) {
            body.append(String.format("  %d. %s%n", i + 1, cardLabel(SpireCard.valueOf(offer.get(i)), true)));
        }

        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < offer.size(); i++) {
            buttons.add(Button.primary(buttonId(ACTION_REWARD, owner, String.valueOf(i)), "[" + (i + 1) + "]"));
        }
        buttons.add(Button.secondary(buttonId(ACTION_REWARD, owner, REWARD_SKIP), "Skip"));

        return new MessageCreateBuilder()
                .addContent(codeBlock(body.toString()))
                .setComponents(ActionRow.of(buttons))
                .build();
    }

    private MessageCreateData renderRest(SpireRun run) {
        String owner = run.getDiscordId();
        String body = String.format(
                "=== Floor %d - Rest Site ===%nYou  HP %d/%d%n%nRest and recover, or thin your deck.",
                run.getCurrentFloor(), run.getPlayerHp(), run.getPlayerMaxHp());
        return new MessageCreateBuilder()
                .addContent(codeBlock(body))
                .setComponents(ActionRow.of(
                        Button.success(buttonId(ACTION_REST, owner, REST_HEAL),
                                "Heal " + SpireService.REST_HEAL_PERCENT + "%"),
                        Button.primary(buttonId(ACTION_REST, owner, REST_REMOVE), "Remove a Card")))
                .build();
    }

    private MessageCreateData renderCardRemoval(SpireRun run, int page) {
        String owner = run.getDiscordId();
        List<String> deck = run.getMasterDeck();
        int totalPages = Math.max(1, (deck.size() + REMOVAL_PAGE_SIZE - 1) / REMOVAL_PAGE_SIZE);
        int clamped = Math.max(0, Math.min(page, totalPages - 1));
        int start = clamped * REMOVAL_PAGE_SIZE;
        int end = Math.min(deck.size(), start + REMOVAL_PAGE_SIZE);

        StringBuilder body = new StringBuilder();
        body.append(String.format("=== Remove a Card ===%n%n"));
        body.append(String.format("Page %d/%d - cards %d-%d of %d:%n",
                clamped + 1, totalPages, start + 1, end, deck.size()));
        for (int i = start; i < end; i++) {
            body.append(String.format("  %d. %s%n", i - start + 1, cardLabel(SpireCard.valueOf(deck.get(i)), false)));
        }

        List<Button> selection = new ArrayList<>();
        for (int i = start; i < end; i++) {
            selection.add(Button.primary(buttonId(ACTION_REMOVE, owner, String.valueOf(i)),
                    "[" + (i - start + 1) + "]"));
        }
        Button prev = Button.secondary(buttonId(ACTION_REMPAGE, owner, String.valueOf(clamped - 1)), "Prev");
        Button next = Button.secondary(buttonId(ACTION_REMPAGE, owner, String.valueOf(clamped + 1)), "Next");
        if (clamped == 0) {
            prev = prev.asDisabled();
        }
        if (clamped >= totalPages - 1) {
            next = next.asDisabled();
        }
        Button cancel = Button.danger(buttonId(ACTION_REMCANCEL, owner, ""), "Cancel");

        return new MessageCreateBuilder()
                .addContent(codeBlock(body.toString()))
                .setComponents(ActionRow.of(selection), ActionRow.of(prev, next, cancel))
                .build();
    }

    private MessageCreateData renderContinuePrompt(SpireRun run) {
        String owner = run.getDiscordId();
        String phaseText = switch (run.getPhase()) {
            case COMBAT -> "in combat";
            case CARD_REWARD -> "choosing a card reward";
            case REST_CHOICE -> "at a rest site";
            case CARD_REMOVAL -> "removing a card";
        };
        String body = String.format(
                "You have a Spire run in progress.%nFloor %d/%d - %s%nHP %d/%d%n%n"
                        + "Continue where you left off, or abandon this run?",
                run.getCurrentFloor(), 8, phaseText, run.getPlayerHp(), run.getPlayerMaxHp());
        return new MessageCreateBuilder()
                .addContent(codeBlock(body))
                .setComponents(ActionRow.of(
                        Button.success(buttonId(ACTION_CONTINUE, owner, ""), "Continue"),
                        Button.danger(buttonId(ACTION_ABANDON, owner, ""), "Abandon")))
                .build();
    }

    // ------------------------------------------------------------------
    // Render helpers
    // ------------------------------------------------------------------

    private String playerStatusLabel(CombatState s) {
        StringBuilder sb = new StringBuilder();
        sb.append("[Block ").append(spireService.statusOf(s.getPlayerEffects(), StatusEffect.BLOCK)).append(']');
        for (StatusEffect effect : List.of(StatusEffect.VULNERABLE, StatusEffect.WEAK, StatusEffect.STRENGTH)) {
            int value = spireService.statusOf(s.getPlayerEffects(), effect);
            if (value != 0) {
                sb.append(" [").append(effect.getLabel()).append(' ').append(value).append(']');
            }
        }
        return sb.toString();
    }

    private String enemyStatusLabel(EnemyInstance e) {
        StringBuilder sb = new StringBuilder();
        for (StatusEffect effect : StatusEffect.values()) {
            int value = spireService.statusOf(e.getEffects(), effect);
            if (value != 0) {
                if (!sb.isEmpty()) {
                    sb.append(' ');
                }
                sb.append('[').append(effect.getLabel()).append(' ').append(value).append(']');
            }
        }
        return sb.toString();
    }

    private static String cardLabel(SpireCard card, boolean annotatePower) {
        String cost = annotatePower && card.getType() == SpireCard.CardType.POWER
                ? card.getCost() + ", Power"
                : String.valueOf(card.getCost());
        return String.format("%s (%s) - %s", card.getDisplayName(), cost, card.getText());
    }

    private static String codeBlock(String body) {
        return "```\n" + body + "\n```";
    }

    private static MessageCreateData plain(String text) {
        return new MessageCreateBuilder().addContent(text).build();
    }

    private static String buttonId(String action, String owner, String arg) {
        return PREFIX + ButtonHandler.DELIMITER + action + ButtonHandler.DELIMITER + owner
                + ButtonHandler.DELIMITER + arg;
    }

    private static Integer parseIndex(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Persistence + message lifecycle
    // ------------------------------------------------------------------

    private void save(SpireRun run) {
        run.setUpdatedAt(Instant.now());
        runRepository.save(run);
    }

    /**
     * Delete-and-repost: acknowledge the button, remove the old game message(s),
     * post a fresh one at the bottom of the channel, and persist its location.
     */
    private void repost(ButtonInteractionEvent event, SpireRun run, MessageCreateData content) {
        event.deferEdit().queue();
        event.getMessage().delete().queue(ok -> { }, err -> { });
        deleteStoredMessage(event, run);
        event.getChannel().sendMessage(content).queue(msg -> {
            run.setCurrentMessageId(msg.getId());
            run.setCurrentChannelId(msg.getChannelId());
            save(run);
        });
    }

    /** Final screen (death/victory) — run is already deleted, so nothing to persist. */
    private void postFinal(ButtonInteractionEvent event, SpireRun run, MessageCreateData content) {
        event.deferEdit().queue();
        event.getMessage().delete().queue(ok -> { }, err -> { });
        deleteStoredMessage(event, run);
        event.getChannel().sendMessage(content).queue();
    }

    /** Best-effort delete of the tracked game message when it isn't the one just clicked. */
    private void deleteStoredMessage(ButtonInteractionEvent event, SpireRun run) {
        String oldId = run.getCurrentMessageId();
        String oldChannel = run.getCurrentChannelId();
        if (oldId == null || oldChannel == null || oldId.equals(event.getMessageId())) {
            return;
        }
        MessageChannel channel = event.getJDA().getChannelById(MessageChannel.class, oldChannel);
        if (channel != null) {
            channel.deleteMessageById(oldId).queue(ok -> { }, err -> { });
        }
    }
}
