package com.ianscottbaker.ianbot.command;

import com.ianscottbaker.ianbot.command.blackjack.BlackjackGame;
import com.ianscottbaker.ianbot.command.blackjack.Card;
import com.ianscottbaker.ianbot.model.IBUser;
import com.ianscottbaker.ianbot.repository.BlackjackGameRepository;
import com.ianscottbaker.ianbot.service.BlackjackService;
import com.ianscottbaker.ianbot.service.UserService;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class Blackjack implements SlashCommand, ButtonHandler, ModalHandler {
    public static final String NAME = "blackjack";
    public static final String PREFIX = "blackjack";
    public static final String ACTION_HIT = "hit";
    public static final String ACTION_STAND = "stand";
    public static final String ACTION_DOUBLE = "double";
    public static final String MODAL_BET = "bet";
    public static final String MODAL_FIELD_BET = "bet_amount";

    private final UserService userService;
    private final BlackjackGameRepository gameRepository;
    private final BlackjackService blackjackService;

    public Blackjack(UserService userService,
                     BlackjackGameRepository gameRepository,
                     BlackjackService blackjackService) {
        this.userService = userService;
        this.gameRepository = gameRepository;
        this.blackjackService = blackjackService;
    }

    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash(NAME, "play blackjack — bet points against the dealer");
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String discordId = event.getUser().getId();
        gameRepository.findByDiscordId(discordId).ifPresentOrElse(
                game -> replyInProgress(event, game, freshPoints(discordId)),
                () -> openBetModal(event, freshPoints(discordId))
        );
    }

    @Override
    public void handle(ModalInteractionEvent event, String payload) {
        if (!MODAL_BET.equals(payload)) {
            event.reply(String.format("uncaught blackjack modal payload %s", payload)).setEphemeral(true).queue();
            return;
        }
        String discordId = event.getUser().getId();
        if (gameRepository.findByDiscordId(discordId).isPresent()) {
            event.reply("You already have an active blackjack game — run /blackjack to view it.").setEphemeral(true).queue();
            return;
        }
        ModalMapping mapping = event.getValue(MODAL_FIELD_BET);
        if (mapping == null) {
            event.reply("Missing bet amount.").setEphemeral(true).queue();
            return;
        }
        int bet;
        try {
            bet = Integer.parseInt(mapping.getAsString().trim());
        } catch (NumberFormatException e) {
            event.reply("Bet must be a whole number.").setEphemeral(true).queue();
            return;
        }
        if (bet < 1) {
            event.reply("Bet must be at least 1.").setEphemeral(true).queue();
            return;
        }
        IBUser user = userService.getOrCreate(discordId);
        if (bet > user.getPoints()) {
            event.reply(String.format("You only have %d points — can't bet %d.", user.getPoints(), bet)).setEphemeral(true).queue();
            return;
        }

        user.setPoints(user.getPoints() - bet);
        userService.save(user);

        BlackjackGame game = blackjackService.newGame(discordId, bet);

        if (blackjackService.isBlackjack(game.getPlayerHand()) || blackjackService.isBlackjack(game.getDealerHand())) {
            resolveAndReply(event, game, user);
            return;
        }

        gameRepository.save(game);
        event.reply(renderInProgress(game, user.getPoints())).setEphemeral(true).queue();
    }

    @Override
    public void handle(ButtonInteractionEvent event, String payload) {
        String discordId = event.getUser().getId();
        BlackjackGame game = gameRepository.findByDiscordId(discordId).orElse(null);
        if (game == null) {
            event.reply("No active blackjack game — run /blackjack to start one.").setEphemeral(true).queue();
            return;
        }
        String action = payload.isEmpty() ? "" : payload.split(ButtonHandler.DELIMITER, 2)[0];
        switch (action) {
            case ACTION_HIT -> doHit(event, game);
            case ACTION_STAND -> doStand(event, game);
            case ACTION_DOUBLE -> doDouble(event, game);
            default -> event.reply(String.format("uncaught blackjack payload %s", payload)).setEphemeral(true).queue();
        }
    }

    private void doHit(ButtonInteractionEvent event, BlackjackGame game) {
        blackjackService.dealToPlayer(game);
        if (blackjackService.isBust(game.getPlayerHand())) {
            IBUser user = userService.getOrCreate(game.getDiscordId());
            resolveAndReply(event, game, user);
            return;
        }
        gameRepository.save(game);
        event.reply(renderInProgress(game, freshPoints(game.getDiscordId()))).setEphemeral(true).queue();
    }

    private void doStand(ButtonInteractionEvent event, BlackjackGame game) {
        blackjackService.playDealer(game);
        IBUser user = userService.getOrCreate(game.getDiscordId());
        resolveAndReply(event, game, user);
    }

    private void doDouble(ButtonInteractionEvent event, BlackjackGame game) {
        IBUser user = userService.getOrCreate(game.getDiscordId());
        if (user.getPoints() < game.getBet()) {
            event.reply(String.format("You need %d more points to double — you have %d.", game.getBet(), user.getPoints())).setEphemeral(true).queue();
            return;
        }
        user.setPoints(user.getPoints() - game.getBet());
        userService.save(user);
        game.setDoubled(true);
        blackjackService.dealToPlayer(game);
        if (!blackjackService.isBust(game.getPlayerHand())) {
            blackjackService.playDealer(game);
        }
        resolveAndReply(event, game, user);
    }

    private void resolveAndReply(IReplyCallback event, BlackjackGame game, IBUser user) {
        BlackjackService.Outcome outcome = blackjackService.settle(game);
        int payout = blackjackService.payout(game, outcome);
        if (payout > 0) {
            user.setPoints(user.getPoints() + payout);
            userService.save(user);
        }
        gameRepository.deleteByDiscordId(game.getDiscordId());
        event.reply(renderResolved(game, outcome, payout, user.getPoints())).setEphemeral(true).queue();
    }

    private void openBetModal(SlashCommandInteractionEvent event, int currentPoints) {
        if (currentPoints < 1) {
            event.reply("You have 0 points — claim some with /claim_points first.").setEphemeral(true).queue();
            return;
        }
        TextInput betInput = TextInput.create(MODAL_FIELD_BET, "Bet amount", TextInputStyle.SHORT)
                .setPlaceholder(String.format("1 to %d", currentPoints))
                .setMinLength(1)
                .setMaxLength(10)
                .setRequired(true)
                .build();
        Modal modal = Modal.create(modalId(MODAL_BET), "Place your bet")
                .addComponents(ActionRow.of(betInput))
                .build();
        event.replyModal(modal).queue();
    }

    private void replyInProgress(SlashCommandInteractionEvent event, BlackjackGame game, int currentPoints) {
        event.reply(renderInProgress(game, currentPoints)).setEphemeral(true).queue();
    }

    private MessageCreateData renderInProgress(BlackjackGame game, int currentPoints) {
        String dealerLine = String.format("Dealer: %s 🂠", game.getDealerHand().get(0).label());
        String playerLine = String.format("You: %s (%d)", cardsString(game.getPlayerHand()), blackjackService.handValue(game.getPlayerHand()));
        String body = String.format("%s%n%s%n%nBet: %d   Points: %d", dealerLine, playerLine, game.getBet(), currentPoints);

        boolean canDouble = game.getPlayerHand().size() == 2 && !game.isDoubled() && currentPoints >= game.getBet();
        Button hit = Button.success(buttonId(ACTION_HIT), "Hit");
        Button stand = Button.danger(buttonId(ACTION_STAND), "Stand");
        Button dbl = Button.primary(buttonId(ACTION_DOUBLE), "Double");
        if (!canDouble) {
            dbl = dbl.asDisabled();
        }
        return new MessageCreateBuilder()
                .addContent(body)
                .setActionRow(hit, stand, dbl)
                .build();
    }

    private MessageCreateData renderResolved(BlackjackGame game, BlackjackService.Outcome outcome, int payout, int newPoints) {
        String dealerLine = String.format("Dealer: %s (%d)", cardsString(game.getDealerHand()), blackjackService.handValue(game.getDealerHand()));
        String playerLine = String.format("You: %s (%d)", cardsString(game.getPlayerHand()), blackjackService.handValue(game.getPlayerHand()));
        int stake = game.isDoubled() ? game.getBet() * 2 : game.getBet();
        int delta = payout - stake;
        String resultLine = switch (outcome) {
            case PLAYER_BLACKJACK -> String.format("Blackjack! You win %d points.", delta);
            case PLAYER_WIN -> String.format("You win %d points.", delta);
            case PUSH -> "Push — bet returned.";
            case DEALER_WIN -> blackjackService.isBust(game.getPlayerHand())
                    ? String.format("Bust — you lose %d points.", stake)
                    : String.format("Dealer wins — you lose %d points.", stake);
        };
        String body = String.format("%s%n%s%n%n%s%nPoints: %d", dealerLine, playerLine, resultLine, newPoints);
        return new MessageCreateBuilder()
                .addContent(body)
                .build();
    }

    private int freshPoints(String discordId) {
        return userService.findExisting(discordId).map(IBUser::getPoints).orElse(0);
    }

    private static String cardsString(List<Card> cards) {
        return cards.stream().map(Card::label).collect(Collectors.joining(" "));
    }

    private static String buttonId(String action) {
        return PREFIX + ButtonHandler.DELIMITER + action;
    }

    private static String modalId(String payload) {
        return PREFIX + ModalHandler.DELIMITER + payload;
    }
}
