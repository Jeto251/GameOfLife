package com.jeto.game.points;

import com.jeto.game.Faction;
import javafx.scene.paint.Color;

public class Point {
    private static long nextId = 1;

    int[] genom;
    boolean isAlive;
    Sex sex;
    int age;
    int ticksLived;  // Счётчик тиков для расчёта возраста
    long id;
    long motherId;
    long fatherId;
    Faction faction;
    double moveChance;  // Шанс движения за тик (0.0 - 1.0)
    int birthCooldown;  // Кулдаун после рождения ребёнка (не может двигаться)
    double strength;    // Базовая сила (0 - 100)
    int combatExp;      // Опыт боя (количество побед)

    public Point() {
        this.id = 0;  // ID присваивается только при рождении
        this.isAlive = false;
        this.sex = Math.random() < 0.5 ? Sex.MALE : Sex.FEMALE;
        this.age = 0;
        this.ticksLived = 0;
        this.motherId = 0;
        this.fatherId = 0;
        this.faction = null;
        this.moveChance = 0.1;  // Шанс движения каждый тик
        this.birthCooldown = 0;
        this.strength = Math.random() * 100;  // 0 - 100
        this.combatExp = 0;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public int isAliveAsInt() {
        return isAlive ? 1 : 0;
    }

    public void setAlive(boolean alive) {
        // Присваиваем ID только при первом оживлении (рождении)
        if (alive && id == 0) {
            id = nextId++;
        }
        isAlive = alive;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getTicksLived() {
        return ticksLived;
    }

    public void setTicksLived(int ticksLived) {
        this.ticksLived = ticksLived;
    }

    public void incrementTick() {
        this.ticksLived++;
        this.age = this.ticksLived / com.jeto.game.Config.TICKS_PER_YEAR;
    }

    public Color getColor() {
        if (!isAlive) {
            return null;
        }

        // Базовый оттенок по фракции (или серый если нет фракции)
        double hue = (faction != null) ? faction.getHue() : 0;

        // Насыщенность — с возрастом уменьшается (становится серее)
        // Женщины чуть светлее
        double baseSaturation = (sex == Sex.FEMALE) ? 0.7 : 0.9;
        double saturation = (faction != null)
            ? Math.max(0.2, baseSaturation - age * 0.007)
            : 0.0;  // Серый если нет фракции

        // Яркость — с возрастом уменьшается (темнеет)
        double baseBrightness = (sex == Sex.FEMALE) ? 1.0 : 0.8;
        double brightness = Math.max(0.3, baseBrightness - age * 0.007);

        return Color.hsb(hue, saturation, brightness);
    }

    public double getDeathProbability() {
        // Базовая вероятность + экспоненциальный рост с возрастом
        double base = 0.001 + Math.pow(age / 80.0, 3) * 0.1;

        // Мужчины умирают чаще
        double sexMultiplier = (sex == Sex.MALE) ? 1.4 : 1.0;

        // Модификатор фракции (меньше = дольше живут)
        double factionModifier = (faction != null) ? faction.getDeathModifier() : 1.0;

        return Math.min(base * sexMultiplier * factionModifier, 1.0);
    }

    public boolean shouldDieNaturally() {
        return Math.random() < getDeathProbability();
    }

    public boolean canReproduce() {
        if (!isAlive) return false;

        if (sex == Sex.FEMALE) {
            return age >= 15 && age <= 45;
        } else {
            return age >= 15 && age <= 60;
        }
    }

    public double getFertility() {
        if (!canReproduce()) return 0;

        if (sex == Sex.FEMALE) {
            if (age <= 24) return 0.25;
            if (age <= 34) return 0.18;
            if (age <= 39) return 0.12;
            return 0.05;
        } else {
            if (age <= 35) return 0.9;
            if (age <= 50) return 0.7;
            return 0.4;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getMotherId() {
        return motherId;
    }

    public void setMotherId(long motherId) {
        this.motherId = motherId;
    }

    public long getFatherId() {
        return fatherId;
    }

    public void setFatherId(long fatherId) {
        this.fatherId = fatherId;
    }

    public void setParents(Point mother, Point father) {
        this.motherId = mother.getId();
        this.fatherId = father.getId();
        this.faction = father.getFaction();  // Дети наследуют фракцию отца

        // Сила наследуется от родителей с мутацией ±10
        double parentAvg = (mother.getStrength() + father.getStrength()) / 2;
        double mutation = (Math.random() - 0.5) * 20;  // -10 to +10
        this.strength = Math.max(0, Math.min(100, parentAvg + mutation));
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public double getStrength() {
        return strength;
    }

    public void setStrength(double strength) {
        this.strength = Math.max(0, Math.min(100, strength));
    }

    public int getCombatExp() {
        return combatExp;
    }

    public void setCombatExp(int combatExp) {
        this.combatExp = combatExp;
    }

    public void addCombatExp() {
        this.combatExp++;
    }

    // Может ли сражаться (мужчина 15+ лет)
    public boolean canFight() {
        return isAlive && sex == Sex.MALE && age >= 15;
    }

    // Боевая мощь = сила * возрастной фактор
    // Пик в 25-35 лет, слабее в молодости и старости
    public double getCombatPower() {
        if (!canFight()) return 0;

        double ageFactor;
        if (age < 20) {
            // Молодой: 15-20 лет, растёт от 0.5 до 0.8
            ageFactor = 0.5 + (age - 15) * 0.06;
        } else if (age <= 35) {
            // Пик: 20-35 лет, 0.9 - 1.0
            ageFactor = 0.9 + (age - 20) * 0.007;
        } else if (age <= 50) {
            // Спад: 35-50 лет, падает от 1.0 до 0.6
            ageFactor = 1.0 - (age - 35) * 0.027;
        } else {
            // Старый: 50+ лет, падает до 0.3
            ageFactor = Math.max(0.3, 0.6 - (age - 50) * 0.015);
        }

        // Бонус от опыта: +5% за каждую победу, макс +50%
        double expBonus = 1.0 + Math.min(combatExp * 0.05, 0.5);

        return (strength / 100.0) * ageFactor * expBonus;
    }

    // Враг ли другой point (другая фракция)
    public boolean isEnemyOf(Point other) {
        if (other == null || faction == null || other.faction == null) {
            return false;
        }
        return !faction.equals(other.faction);
    }

    // Проверяет, можно ли этой паре иметь детей (не родственники)
    public boolean canMateWith(Point other) {
        if (other == null) return false;

        // Проверяем: я не ребёнок партнёра и партнёр не мой ребёнок
        if (this.motherId == other.id || this.fatherId == other.id) {
            return false; // other — мой родитель
        }
        if (other.motherId == this.id || other.fatherId == this.id) {
            return false; // я — родитель other
        }

        // Проверяем: мы не сиблинги (общие родители)
        // motherId > 0 означает, что это не первое поколение
        if (this.motherId > 0 && this.motherId == other.motherId &&
            this.fatherId == other.fatherId) {
            return false; // сиблинги
        }

        return true;
    }

    public double getMoveChance() {
        return moveChance;
    }

    public void setMoveChance(double moveChance) {
        this.moveChance = moveChance;
    }

    public int getBirthCooldown() {
        return birthCooldown;
    }

    public void setBirthCooldown(int birthCooldown) {
        this.birthCooldown = birthCooldown;
    }

    // Проверяет, должна ли клетка двигаться на этом тике
    public boolean shouldMove() {
        if (!isAlive || moveChance <= 0) return false;
        if (birthCooldown > 0) return false;  // Не двигаемся после рождения ребёнка
        double factionBonus = (faction != null) ? faction.getMoveBonus() : 1.0;
        return Math.random() < moveChance * factionBonus;
    }
}


