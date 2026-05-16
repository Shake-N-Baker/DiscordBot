package com.ianscottbaker.ianbot.command.spire;

/**
 * The interaction state a {@link SpireRun} is currently sitting in. Drives which
 * render + button set the player sees. Death and victory are not phases — the run
 * document is deleted outright.
 */
public enum Phase {
    COMBAT,
    CARD_REWARD,
    REST_CHOICE,
    CARD_REMOVAL
}
