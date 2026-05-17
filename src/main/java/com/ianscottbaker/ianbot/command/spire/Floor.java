package com.ianscottbaker.ianbot.command.spire;

/**
 * Encounter type of a floor. The floor-number to type mapping for a v1 run lives
 * here so the rest sites are encoded in exactly one place.
 */
public enum Floor {
    COMBAT,
    REST,
    BOSS;

    public static final int TOTAL_FLOORS = 9;
    public static final int FIRST_REST_FLOOR = 4;
    public static final int SECOND_REST_FLOOR = 8;

    public static Floor forNumber(int floor) {
        if (floor >= TOTAL_FLOORS) {
            return BOSS;
        }
        if (floor == FIRST_REST_FLOOR || floor == SECOND_REST_FLOOR) {
            return REST;
        }
        return COMBAT;
    }
}
