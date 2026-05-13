package com.ianscottbaker.ianbot.service;

import com.ianscottbaker.ianbot.command.blackjack.BlackjackGame;
import com.ianscottbaker.ianbot.command.blackjack.Card;
import com.ianscottbaker.ianbot.command.blackjack.Rank;
import com.ianscottbaker.ianbot.command.blackjack.Suit;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
public class BlackjackService {
    public static final int DEALER_STAND_VALUE = 17;
    public static final int BLACKJACK_VALUE = 21;

    public enum Outcome {
        PLAYER_BLACKJACK,
        PLAYER_WIN,
        DEALER_WIN,
        PUSH
    }

    private final Random random = new SecureRandom();

    public BlackjackGame newGame(String discordId, int bet) {
        BlackjackGame game = new BlackjackGame(discordId, bet);
        game.setDeck(freshShuffledDeck());
        game.getPlayerHand().add(drawTop(game));
        game.getDealerHand().add(drawTop(game));
        game.getPlayerHand().add(drawTop(game));
        game.getDealerHand().add(drawTop(game));
        return game;
    }

    public Card dealToPlayer(BlackjackGame game) {
        Card card = drawTop(game);
        game.getPlayerHand().add(card);
        return card;
    }

    public void playDealer(BlackjackGame game) {
        while (handValue(game.getDealerHand()) < DEALER_STAND_VALUE) {
            game.getDealerHand().add(drawTop(game));
        }
    }

    public int handValue(List<Card> hand) {
        int total = 0;
        int aces = 0;
        for (Card card : hand) {
            total += card.getRank().getBaseValue();
            if (card.getRank() == Rank.ACE) {
                aces++;
            }
        }
        while (total > BLACKJACK_VALUE && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public boolean isBlackjack(List<Card> hand) {
        return hand.size() == 2 && handValue(hand) == BLACKJACK_VALUE;
    }

    public boolean isBust(List<Card> hand) {
        return handValue(hand) > BLACKJACK_VALUE;
    }

    public Outcome settle(BlackjackGame game) {
        List<Card> player = game.getPlayerHand();
        List<Card> dealer = game.getDealerHand();
        if (isBust(player)) {
            return Outcome.DEALER_WIN;
        }
        boolean playerBlackjack = isBlackjack(player);
        boolean dealerBlackjack = isBlackjack(dealer);
        if (playerBlackjack && dealerBlackjack) {
            return Outcome.PUSH;
        }
        if (playerBlackjack) {
            return Outcome.PLAYER_BLACKJACK;
        }
        if (dealerBlackjack) {
            return Outcome.DEALER_WIN;
        }
        if (isBust(dealer)) {
            return Outcome.PLAYER_WIN;
        }
        int playerTotal = handValue(player);
        int dealerTotal = handValue(dealer);
        if (playerTotal > dealerTotal) {
            return Outcome.PLAYER_WIN;
        }
        if (playerTotal < dealerTotal) {
            return Outcome.DEALER_WIN;
        }
        return Outcome.PUSH;
    }

    /**
     * Points to credit back to the user. Bet was already deducted up front, so
     * this is total return: stake + winnings on a win, stake-only on a push, 0 on a loss.
     */
    public int payout(BlackjackGame game, Outcome outcome) {
        int stake = game.isDoubled() ? game.getBet() * 2 : game.getBet();
        return switch (outcome) {
            case PLAYER_BLACKJACK -> game.getBet() + (game.getBet() * 3 / 2);
            case PLAYER_WIN -> stake * 2;
            case PUSH -> stake;
            case DEALER_WIN -> 0;
        };
    }

    private Card drawTop(BlackjackGame game) {
        return game.getDeck().remove(game.getDeck().size() - 1);
    }

    private List<Card> freshShuffledDeck() {
        List<Card> deck = new ArrayList<>(52);
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck.add(new Card(rank, suit));
            }
        }
        Collections.shuffle(deck, random);
        return deck;
    }
}
