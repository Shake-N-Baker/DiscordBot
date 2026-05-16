package com.ianscottbaker.ianbot.command.spire;

/**
 * Encounter type of a floor. The floor-number to type mapping for a v1 run lives
 * here so floor 4's rest site is encoded in exactly one place.
 */
public enum Floor {
    COMBAT,
    REST,
    BOSS;

    public static final int TOTAL_FLOORS = 8;
    public static final int REST_FLOOR = 4;

    public static Floor forNumber(int floor) {
        if (floor >= TOTAL_FLOORS) {
            return BOSS;
        }
        if (floor == REST_FLOOR) {
            return REST;
        }
        return COMBAT;
    }
}
