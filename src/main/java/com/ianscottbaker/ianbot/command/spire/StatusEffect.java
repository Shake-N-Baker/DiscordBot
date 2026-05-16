package com.ianscottbaker.ianbot.command.spire;

/**
 * The four v1 status effects. Held by players and enemies alike in a
 * {@code Map<StatusEffect, Integer>} of stack counts, so combat math is uniform
 * across both. BLOCK is modelled as an effect too; it just resets each turn.
 */
public enum StatusEffect {
    BLOCK("Block"),
    VULNERABLE("Vuln"),
    WEAK("Weak"),
    STRENGTH("Str");

    private final String label;

    StatusEffect(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
