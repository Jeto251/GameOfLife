package com.jeto.game;

public class Config {
    public static final int WINDOW_HEIGHT = 1200;
    public static final int WINDOW_WIDTH = 1200;

    public static final int CELL_SIZE = 5; //px
    public static final int TICKS_PER_YEAR = 2; // Сколько тиков = 1 год

    // Фракции: name, hue, attack, defense, fertility, twins, move, exp, death
    public static final Faction[] FACTIONS = {
            new Faction("Red", 0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
            new Faction("Green", 120, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
            new Faction("Blue", 210, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
            new Faction("Purple", 290, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    };

    public static Faction getRandomFaction() {
        return FACTIONS[(int) (Math.random() * FACTIONS.length)];
    }
}
