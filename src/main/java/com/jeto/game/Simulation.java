package com.jeto.game;

import com.jeto.game.points.Point;
import com.jeto.game.points.Sex;

import java.util.ArrayList;
import java.util.List;

public class Simulation {
    private static final double TWINS_CHANCE = 0.1;     // Шанс двойни
    private static final double TRIPLETS_CHANCE = 0.05; // Шанс тройни
    private static final double PAIR_MOVE_CHANCE = 0.03; // Шанс, что устоявшаяся пара разойдется

    private int width;
    private int height;

    private Point[][] board;

    public Simulation(int width, int height) {
        this.width = width;
        this.height = height;

        this.board = new Point[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.board[x][y] = new Point();
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void printBoard() {
        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder("|");
            for (int x = 0; x < width; x++) {
                if (this.board[x][y].isAlive()) {
                    line.append("*");
                } else {
                    line.append(".");
                }
            }
            line.append("|");
            System.out.println(line);
        }
        System.out.println("---\n");
    }

    public void setAlive(int x, int y) {
        setState(x, y, 1);
    }

    public void setAlive(int x, int y, Faction faction, Sex sex) {
        x = Math.floorMod(x, width);
        y = Math.floorMod(y, height);
        Point point = this.board[x][y];
        point.setAlive(true);
        point.setFaction(faction);
        if (sex != null) {
            point.setSex(sex);
        }
        // Случайный возраст 15-40 лет
        int randomAge = 15 + (int)(Math.random() * 26);
        point.setAge(randomAge);
        point.setTicksLived(randomAge * Config.TICKS_PER_YEAR);
    }

    public void setDead(int x, int y) {
        setState(x, y, 0);
    }

    public int countAliveNeibs(int x, int y) {
        int count = 0;

        count += getState(x - 1, y - 1);
        count += getState(x, y - 1);
        count += getState(x + 1, y - 1);

        count += getState(x - 1, y);
        count += getState(x + 1, y);

        count += getState(x - 1, y + 1);
        count += getState(x, y + 1);
        count += getState(x + 1, y + 1);

        return count;
    }

    public int getState(int x, int y) {
        x = Math.floorMod(x, width);
        y = Math.floorMod(y, height);

        return this.board[x][y].isAliveAsInt();
    }

    public void setState(int x, int y, int state) {
        x = Math.floorMod(x, width);
        y = Math.floorMod(y, height);

        this.board[x][y].setAlive(state == 1);
    }

    public boolean isDead(int x, int y) {
        return getState(x, y) == 0;
    }

    public Point getPoint(int x, int y) {
        x = Math.floorMod(x, width);
        y = Math.floorMod(y, height);
        return this.board[x][y];
    }

    private static final int[][] OFFSETS = {
        {-1, -1}, {0, -1}, {1, -1},
        {-1, 0},          {1, 0},
        {-1, 1},  {0, 1},  {1, 1}
    };

    // Проверяет, есть ли у клетки подходящий партнёр рядом
    private boolean hasPartnerNearby(Point point, int x, int y, Point[][] board) {
        if (!point.isAlive()) {
            return false;
        }

        Sex targetSex = (point.getSex() == Sex.MALE) ? Sex.FEMALE : Sex.MALE;

        for (int[] offset : OFFSETS) {
            int nx = Math.floorMod(x + offset[0], width);
            int ny = Math.floorMod(y + offset[1], height);
            Point neighbor = board[nx][ny];

            // Остаёмся с партнёром противоположного пола (не родственником)
            if (neighbor.isAlive() && neighbor.getSex() == targetSex &&
                point.canMateWith(neighbor)) {
                return true;
            }
        }
        return false;
    }

    // Ищет лучшего партнёра-мужчину рядом с женщиной (половой отбор по силе)
    private Point findBestMalePartner(Point female, int femaleX, int femaleY, Point[][] board) {
        Point bestMale = null;
        double bestScore = 0;

        for (int[] offset : OFFSETS) {
            int nx = Math.floorMod(femaleX + offset[0], width);
            int ny = Math.floorMod(femaleY + offset[1], height);
            Point neighbor = board[nx][ny];
            if (neighbor.canReproduce() && neighbor.getSex() == Sex.MALE) {
                // Проверяем, что это не родитель-ребёнок
                if (female.canMateWith(neighbor)) {
                    // Половой отбор: боевая мощь + фертильность
                    double score = neighbor.getCombatPower() * 100
                                 + neighbor.getFertility() * 50;
                    if (score > bestScore) {
                        bestScore = score;
                        bestMale = neighbor;
                    }
                }
            }
        }
        return bestMale;
    }

    // Проверяет, есть ли рядом союзная женщина (для бонуса защиты)
    private boolean hasAlliedFemaleNearby(Point point, int x, int y, Point[][] board) {
        if (point.getFaction() == null) return false;

        for (int[] offset : OFFSETS) {
            int nx = Math.floorMod(x + offset[0], width);
            int ny = Math.floorMod(y + offset[1], height);
            Point neighbor = board[nx][ny];

            if (neighbor.isAlive() && neighbor.getSex() == Sex.FEMALE &&
                neighbor.getFaction() != null &&
                neighbor.getFaction().equals(point.getFaction())) {
                return true;
            }
        }
        return false;
    }

    // Находит ближайшего врага в радиусе (для движения к нему)
    private int[] findNearestEnemy(Point point, int x, int y, Point[][] board, int radius) {
        if (point.getFaction() == null || !point.canFight()) return null;

        int[] nearest = null;
        double minDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = Math.floorMod(x + dx, width);
                int ny = Math.floorMod(y + dy, height);
                Point neighbor = board[nx][ny];

                if (neighbor.isAlive() && neighbor.canFight() && point.isEnemyOf(neighbor)) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = new int[]{nx, ny};
                    }
                }
            }
        }
        return nearest;
    }

    // Находит ближайшую союзную женщину в радиусе (для движения к ней)
    private int[] findNearestAlliedFemale(Point point, int x, int y, Point[][] board, int radius) {
        if (point.getFaction() == null || point.getSex() != Sex.MALE) return null;

        int[] nearest = null;
        double minDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = Math.floorMod(x + dx, width);
                int ny = Math.floorMod(y + dy, height);
                Point neighbor = board[nx][ny];

                if (neighbor.isAlive() && neighbor.getSex() == Sex.FEMALE &&
                    neighbor.getFaction() != null && neighbor.getFaction().equals(point.getFaction())) {
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = new int[]{nx, ny};
                    }
                }
            }
        }
        return nearest;
    }

    // Находит клетку ближе к цели
    private int[] findCellTowards(int fromX, int fromY, int toX, int toY, Point[][] board) {
        int bestX = fromX;
        int bestY = fromY;
        double bestDist = Double.MAX_VALUE;

        for (int[] offset : OFFSETS) {
            int nx = Math.floorMod(fromX + offset[0], width);
            int ny = Math.floorMod(fromY + offset[1], height);

            if (!board[nx][ny].isAlive()) {
                // Расстояние с учётом тороидальности
                int dx = Math.min(Math.abs(nx - toX), width - Math.abs(nx - toX));
                int dy = Math.min(Math.abs(ny - toY), height - Math.abs(ny - toY));
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < bestDist) {
                    bestDist = dist;
                    bestX = nx;
                    bestY = ny;
                }
            }
        }

        if (bestX == fromX && bestY == fromY) {
            return null; // Нет свободных клеток ближе
        }
        return new int[]{bestX, bestY};
    }

    // Проверяет, есть ли враги в радиусе N клеток
    private boolean hasEnemiesNearby(Point point, int x, int y, Point[][] board, int radius) {
        if (point.getFaction() == null) return false;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx == 0 && dy == 0) continue;

                int nx = Math.floorMod(x + dx, width);
                int ny = Math.floorMod(y + dy, height);
                Point neighbor = board[nx][ny];

                if (neighbor.isAlive() && neighbor.getFaction() != null &&
                    !neighbor.getFaction().equals(point.getFaction())) {
                    return true;
                }
            }
        }
        return false;
    }

    // Находит пустые клетки рядом с позицией (для рождения)
    private List<int[]> findEmptyCellsNear(int x, int y, Point[][] board) {
        List<int[]> emptyCells = new ArrayList<>();

        for (int[] offset : OFFSETS) {
            int nx = Math.floorMod(x + offset[0], width);
            int ny = Math.floorMod(y + offset[1], height);
            if (!board[nx][ny].isAlive()) {
                emptyCells.add(new int[]{nx, ny});
            }
        }
        return emptyCells;
    }

    public void step() {
        Point[][] newBoard = new Point[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                newBoard[x][y] = new Point();
            }
        }

        // Первый проход: выживание/смерть (без перемещения)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point oldPoint = this.board[x][y];

                if (oldPoint.isAlive() && !oldPoint.shouldDieNaturally()) {
                    Point newPoint = newBoard[x][y];
                    // Сначала копируем ID, чтобы setAlive не создал новый
                    newPoint.setId(oldPoint.getId());
                    newPoint.setAlive(true);
                    newPoint.setSex(oldPoint.getSex());
                    newPoint.setTicksLived(oldPoint.getTicksLived());
                    newPoint.incrementTick();  // Увеличивает ticksLived и пересчитывает age
                    newPoint.setMotherId(oldPoint.getMotherId());
                    newPoint.setFatherId(oldPoint.getFatherId());
                    newPoint.setFaction(oldPoint.getFaction());
                    newPoint.setMoveChance(oldPoint.getMoveChance());
                    newPoint.setStrength(oldPoint.getStrength());
                    newPoint.setCombatExp(oldPoint.getCombatExp());
                    // Уменьшаем кулдаун рождения
                    int cooldown = oldPoint.getBirthCooldown();
                    newPoint.setBirthCooldown(cooldown > 0 ? cooldown - 1 : 0);
                }
            }
        }

        // Второй проход: бой между мужчинами разных фракций
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point point = newBoard[x][y];
                if (!point.canFight()) {
                    continue;
                }

                // Ищем врагов рядом
                for (int[] offset : OFFSETS) {
                    int nx = Math.floorMod(x + offset[0], width);
                    int ny = Math.floorMod(y + offset[1], height);
                    Point neighbor = newBoard[nx][ny];

                    if (neighbor.canFight() && point.isEnemyOf(neighbor)) {
                        double power1 = point.getCombatPower();
                        double power2 = neighbor.getCombatPower();

                        // Применяем бонусы фракций (атака vs защита)
                        if (point.getFaction() != null) {
                            power1 *= point.getFaction().getAttackBonus();
                        }
                        if (neighbor.getFaction() != null) {
                            power2 *= neighbor.getFaction().getDefenseBonus();
                        }

                        double winChance = power1 / (power1 + power2);

                        if (Math.random() > winChance) {
                            point.setAlive(false);
                            if (point.getFaction() != null) {
                                point.getFaction().addDeath();
                            }
                            // Опыт с бонусом фракции
                            int expGain = neighbor.getFaction() != null
                                ? (int) Math.ceil(neighbor.getFaction().getExpBonus())
                                : 1;
                            for (int i = 0; i < expGain; i++) {
                                neighbor.addCombatExp();
                            }
                            break;  // Этот умер, выходим из цикла
                        } else {
                            neighbor.setAlive(false);
                            if (neighbor.getFaction() != null) {
                                neighbor.getFaction().addDeath();
                            }
                            // Опыт с бонусом фракции
                            int expGain = point.getFaction() != null
                                ? (int) Math.ceil(point.getFaction().getExpBonus())
                                : 1;
                            for (int i = 0; i < expGain; i++) {
                                point.addCombatExp();
                            }
                        }
                    }
                }
            }
        }

        // Третий проход: рождение от фертильных женщин
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point female = newBoard[x][y];

                if (!female.canReproduce() || female.getSex() != Sex.FEMALE) {
                    continue;
                }

                // Не размножаемся если враги в радиусе 1 клеток
//                if (hasEnemiesNearby(female, x, y, newBoard, 1)) {
//                    continue;
//                }

                // Ищем партнёра-мужчину рядом (не родственника)
                Point male = findBestMalePartner(female, x, y, newBoard);
                if (male == null) {
                    continue;
                }

                // Вероятность зачатия с бонусом фракции
                double fertilityBonus = female.getFaction() != null
                    ? female.getFaction().getFertilityBonus()
                    : 1.0;
                double birthProbability = female.getFertility() * male.getFertility() * fertilityBonus;
                if (Math.random() >= birthProbability) {
                    continue;
                }

                // Ищем пустые клетки рядом с матерью
                List<int[]> emptyCells = findEmptyCellsNear(x, y, newBoard);
                if (emptyCells.isEmpty()) {
                    continue;
                }

                // Определяем количество детей (1, 2 или 3) с бонусом фракции
                double twinsBonus = male.getFaction() != null
                    ? male.getFaction().getTwinsBonus()
                    : 1.0;
                int childCount = 1;
                double roll = Math.random();
                if (roll < TRIPLETS_CHANCE * twinsBonus) {
                    childCount = 3;
                } else if (roll < TWINS_CHANCE * twinsBonus) {
                    childCount = 2;
                }

                // Рождаем детей (сколько позволяют пустые клетки)
                int actualBirths = Math.min(childCount, emptyCells.size());
                for (int i = 0; i < actualBirths; i++) {
                    int cellIndex = (int) (Math.random() * emptyCells.size());
                    int[] birthCell = emptyCells.remove(cellIndex);
                    Point child = newBoard[birthCell[0]][birthCell[1]];
                    child.setAlive(true);
                    child.setParents(female, male);
                }

                // Устанавливаем кулдаун родителям — они не смогут двигаться
                female.setBirthCooldown(20);
                male.setBirthCooldown(10);
            }
        }

        // Третий проход: перемещение (после рождения, чтобы учесть кулдаун)
        Point[][] finalBoard = new Point[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                finalBoard[x][y] = new Point();
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Point point = newBoard[x][y];

                if (point.isAlive()) {
                    int targetX = x;
                    int targetY = y;

                    // Приоритет 1: Враг в радиусе 2 клеток — идём атаковать (даже неподвижные)
                    int[] enemyPos = findNearestEnemy(point, x, y, newBoard, 3);
                    if (enemyPos != null) {
                        int[] moveTarget = findCellTowards(x, y, enemyPos[0], enemyPos[1], finalBoard);
                        if (moveTarget != null) {
                            targetX = moveTarget[0];
                            targetY = moveTarget[1];
                        }
                    }
                    // Приоритет 2: Союзная женщина в радиусе 3 клеток (только для мужчин без партнёра)
                    else if (point.getSex() == Sex.MALE && !hasPartnerNearby(point, x, y, newBoard)) {
                        int[] femalePos = findNearestAlliedFemale(point, x, y, newBoard, 10);
                        if (femalePos != null && point.shouldMove()) {
                            int[] moveTarget = findCellTowards(x, y, femalePos[0], femalePos[1], finalBoard);
                            if (moveTarget != null) {
                                targetX = moveTarget[0];
                                targetY = moveTarget[1];
                            }
                        }
                    }
                    // Приоритет 3: Обычное случайное движение
                    else {
                        boolean hasPartner = hasPartnerNearby(point, x, y, newBoard);
                        boolean shouldMove;

                        if (hasPartner) {
                            shouldMove = point.getBirthCooldown() == 0 && Math.random() < PAIR_MOVE_CHANCE;
                        } else {
                            shouldMove = point.shouldMove();
                        }

                        if (shouldMove) {
                            List<int[]> emptyCells = findEmptyCellsNear(x, y, finalBoard);
                            if (!emptyCells.isEmpty()) {
                                int[] target = emptyCells.get((int) (Math.random() * emptyCells.size()));
                                targetX = target[0];
                                targetY = target[1];
                            }
                        }
                    }

                    // Копируем в финальную позицию
                    Point finalPoint = finalBoard[targetX][targetY];
                    // Сначала ID, чтобы setAlive не создал новый
                    finalPoint.setId(point.getId());
                    finalPoint.setAlive(true);
                    finalPoint.setSex(point.getSex());
                    finalPoint.setTicksLived(point.getTicksLived());
                    finalPoint.setAge(point.getAge());
                    finalPoint.setMotherId(point.getMotherId());
                    finalPoint.setFatherId(point.getFatherId());
                    finalPoint.setFaction(point.getFaction());
                    finalPoint.setMoveChance(point.getMoveChance());
                    finalPoint.setStrength(point.getStrength());
                    finalPoint.setCombatExp(point.getCombatExp());
                    finalPoint.setBirthCooldown(point.getBirthCooldown());
                }
            }
        }

        this.board = finalBoard;
    }

    public void resize(int newWidth, int newHeight) {
        Point[][] newBoard = new Point[newWidth][newHeight];
        for (int x = 0; x < newWidth; x++) {
            for (int y = 0; y < newHeight; y++) {
                newBoard[x][y] = new Point();
            }
        }

        int copyWidth = Math.min(width, newWidth);
        int copyHeight = Math.min(height, newHeight);

        for (int x = 0; x < copyWidth; x++) {
            for (int y = 0; y < copyHeight; y++) {
                Point oldPoint = this.board[x][y];
                Point newPoint = newBoard[x][y];
                newPoint.setId(oldPoint.getId());
                newPoint.setAlive(oldPoint.isAlive());
                newPoint.setSex(oldPoint.getSex());
                newPoint.setTicksLived(oldPoint.getTicksLived());
                newPoint.setAge(oldPoint.getAge());
                newPoint.setMotherId(oldPoint.getMotherId());
                newPoint.setFatherId(oldPoint.getFatherId());
                newPoint.setFaction(oldPoint.getFaction());
                newPoint.setMoveChance(oldPoint.getMoveChance());
                newPoint.setStrength(oldPoint.getStrength());
                newPoint.setCombatExp(oldPoint.getCombatExp());
                newPoint.setBirthCooldown(oldPoint.getBirthCooldown());
            }
        }

        this.board = newBoard;
        this.width = newWidth;
        this.height = newHeight;
    }

    public static void main(String[] args) {
        Simulation simulation = new Simulation(8, 5);

        for (int i = 0; i < 3; i++) {
            simulation.printBoard();
            simulation.step();
        }
    }
}
