package com.ianscottbaker.ianbot.command.blackjack;

public enum Rank {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(10, "J"),
    QUEEN(10, "Q"),
    KING(10, "K"),
    ACE(11, "A");

    private final int baseValue;
    private final String label;

    Rank(int baseValue, String label) {
        this.baseValue = baseValue;
        this.label = label;
    }

    public int getBaseValue() {
        return baseValue;
    }

    public String getLabel() {
        return label;
    }
}
