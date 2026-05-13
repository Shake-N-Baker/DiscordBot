package com.ianscottbaker.ianbot.command.blackjack;

public enum Suit {
    CLUBS(":club_suit:"),
    DIAMONDS(":diamond_suit:"),
    HEARTS(":heart_suit:"),
    SPADES(":spade_suit:");

    private final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
