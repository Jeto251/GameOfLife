package com.jeto.game;

import javafx.scene.paint.Color;

public class Faction {
    private static int nextId = 0;

    private final int id;
    private final String name;
    private final double hue;  // Базовый оттенок (0-360)
    private int deaths;  // Счётчик погибших

    // Модификаторы фракции
    private final double attackBonus;      // Бонус к атаке в бою
    private final double defenseBonus;     // Бонус к защите (шанс выжить)
    private final double fertilityBonus;   // Бонус к фертильности
    private final double twinsBonus;       // Множитель шанса двойни/тройни
    private final double moveBonus;        // Бонус к шансу движения
    private final double expBonus;         // Бонус к получению опыта
    private final double deathModifier;    // Множитель смертности (меньше = дольше живут)

    public Faction(String name, double hue,
                   double attackBonus, double defenseBonus,
                   double fertilityBonus, double twinsBonus,
                   double moveBonus, double expBonus, double deathModifier) {
        this.id = nextId++;
        this.name = name;
        this.hue = hue;
        this.deaths = 0;
        this.attackBonus = attackBonus;
        this.defenseBonus = defenseBonus;
        this.fertilityBonus = fertilityBonus;
        this.twinsBonus = twinsBonus;
        this.moveBonus = moveBonus;
        this.expBonus = expBonus;
        this.deathModifier = deathModifier;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getHue() {
        return hue;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeath() {
        deaths++;
    }

    public void resetDeaths() {
        deaths = 0;
    }

    public double getAttackBonus() {
        return attackBonus;
    }

    public double getDefenseBonus() {
        return defenseBonus;
    }

    public double getFertilityBonus() {
        return fertilityBonus;
    }

    public double getTwinsBonus() {
        return twinsBonus;
    }

    public double getMoveBonus() {
        return moveBonus;
    }

    public double getExpBonus() {
        return expBonus;
    }

    public double getDeathModifier() {
        return deathModifier;
    }

    public Color getColor(double saturation, double brightness) {
        return Color.hsb(hue, saturation, brightness);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Faction faction = (Faction) obj;
        return id == faction.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return name;
    }
}
