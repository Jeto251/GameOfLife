package com.jeto.game;

import com.jeto.game.points.Point;
import javafx.application.Platform;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import static com.jeto.game.Config.*;

public class MainView extends BorderPane {

    private Button startButton;
    private Button stepButton;
    private Button clearButton;
    private Slider speedSlider;
    private Label speedLabel;
    private ComboBox<Faction> factionComboBox;
    private ComboBox<String> sexComboBox;
    private Canvas canvas;
    private HBox controls;
    private VBox infoPanel;
    private HBox statsPanel;
    private Label coordsLabel;
    private Label aliveLabel;
    private Label sexLabel;
    private Label ageLabel;
    private Label idLabel;
    private Label motherIdLabel;
    private Label fatherIdLabel;
    private Label fertilityLabel;
    private Label moveChanceLabel;
    private Label birthCooldownLabel;
    private Label factionLabel;
    private Label strengthLabel;
    private Label combatPowerLabel;
    private Label combatExpLabel;

    // Stats labels
    private Label populationLabel;
    private Label maleCountLabel;
    private Label femaleCountLabel;
    private Label avgAgeLabel;
    private Label avgFertilityLabel;

    // Faction stats table
    private Label[] factionTotalLabels;
    private Label[] factionMaleLabels;
    private Label[] factionFemaleLabels;
    private Label[] factionStrengthLabels;
    private Label[] factionCombatLabels;
    private Label[] factionMaxExpLabels;
    private Label[] factionAvgExpLabels;
    private Label[] factionDeathsLabels;

    private Affine affine;
    private Simulation simulation;
    private int selectedX = -1;
    private int selectedY = -1;

    private int drawMode = 0;

    private AnimationTimer gameLoop;
    private boolean running = false;
    private long lastUpdate = 0;
    private long updateInterval = 50_000_000; // 50ms (Speed 10 по умолчанию)

    public MainView() {
        // Панель управления
        this.startButton = new Button("Start");
        this.startButton.setOnAction(e -> {
            running = !running;
            startButton.setText(running ? "Stop" : "Start");
        });

        this.stepButton = new Button("Step");
        this.stepButton.setOnAction(e -> {
            simulation.step();
            updateCellInfo();
            updateStats();
        });

        this.clearButton = new Button("Clear");
        this.clearButton.setOnAction(e -> {
            running = false;
            startButton.setText("Start");
            int w = simulation.getWidth();
            int h = simulation.getHeight();
            simulation = new Simulation(w, h);
            for (Faction faction : FACTIONS) {
                faction.resetDeaths();
            }
            updateStats();
        });

        this.speedLabel = new Label("Speed: 10");
        this.speedSlider = new Slider(1, 100, 10);
        this.speedSlider.setShowTickMarks(true);
        this.speedSlider.setMajorTickUnit(20);
        this.speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int speed = newVal.intValue();
            speedLabel.setText("Speed: " + speed);
            // Скорость 1 = 500ms, скорость 100 = 1ms
            updateInterval = (long) (500_000_000.0 / speed);
        });

        this.factionComboBox = new ComboBox<>();
        this.factionComboBox.getItems().addAll(FACTIONS);
        this.factionComboBox.setValue(FACTIONS[0]);

        this.sexComboBox = new ComboBox<>();
        this.sexComboBox.getItems().addAll("Random", "M", "F");
        this.sexComboBox.setValue("Random");

        this.controls = new HBox(15);
        this.controls.setPadding(new Insets(10));
        this.controls.setStyle("-fx-background-color: #e0e0e0;");
        this.controls.getChildren().addAll(startButton, stepButton, clearButton, speedLabel, speedSlider, new Label("Faction:"), factionComboBox, new Label("Sex:"), sexComboBox);

        // Cell info panel
        this.coordsLabel = new Label("Position: -");
        this.aliveLabel = new Label("State: -");
        this.idLabel = new Label("ID: -");
        this.sexLabel = new Label("Sex: -");
        this.ageLabel = new Label("Age: -");
        this.fertilityLabel = new Label("Fertility: -");
        this.moveChanceLabel = new Label("Move chance: -");
        this.birthCooldownLabel = new Label("Birth cooldown: -");
        this.motherIdLabel = new Label("Mother ID: -");
        this.fatherIdLabel = new Label("Father ID: -");
        this.factionLabel = new Label("Faction: -");
        this.strengthLabel = new Label("Strength: -");
        this.combatPowerLabel = new Label("Combat: -");
        this.combatExpLabel = new Label("Exp: -");

        this.infoPanel = new VBox(8);
        this.infoPanel.setPadding(new Insets(10));
        this.infoPanel.setStyle("-fx-background-color: #f0f0f0;");
        this.infoPanel.setPrefWidth(160);
        this.infoPanel.getChildren().addAll(
                new Label("Cell Info:"),
                coordsLabel,
                aliveLabel,
                idLabel,
                factionLabel,
                sexLabel,
                ageLabel,
                strengthLabel,
                combatPowerLabel,
                combatExpLabel,
                fertilityLabel,
                moveChanceLabel,
                birthCooldownLabel,
                motherIdLabel,
                fatherIdLabel
        );

        // Stats panel (bottom)
        this.populationLabel = new Label("Population: 0");
        this.maleCountLabel = new Label("Males: 0");
        this.femaleCountLabel = new Label("Females: 0");
        this.avgAgeLabel = new Label("Avg Age: 0");
        this.avgFertilityLabel = new Label("Avg Fertility: 0%");

        // Faction stats table
        this.factionTotalLabels = new Label[FACTIONS.length];
        this.factionMaleLabels = new Label[FACTIONS.length];
        this.factionFemaleLabels = new Label[FACTIONS.length];
        this.factionStrengthLabels = new Label[FACTIONS.length];
        this.factionCombatLabels = new Label[FACTIONS.length];
        this.factionMaxExpLabels = new Label[FACTIONS.length];
        this.factionAvgExpLabels = new Label[FACTIONS.length];
        this.factionDeathsLabels = new Label[FACTIONS.length];

        GridPane factionTable = new GridPane();
        factionTable.setHgap(15);
        factionTable.setVgap(3);

        // Header row
        factionTable.add(new Label("Faction"), 0, 0);
        factionTable.add(new Label("Total"), 1, 0);
        factionTable.add(new Label("M"), 2, 0);
        factionTable.add(new Label("F"), 3, 0);
        factionTable.add(new Label("Str"), 4, 0);
        factionTable.add(new Label("Combat"), 5, 0);
        factionTable.add(new Label("MaxExp"), 6, 0);
        factionTable.add(new Label("AvgExp"), 7, 0);
        factionTable.add(new Label("Deaths"), 8, 0);

        // Faction rows
        for (int i = 0; i < FACTIONS.length; i++) {
            factionTotalLabels[i] = new Label("0");
            factionMaleLabels[i] = new Label("0");
            factionFemaleLabels[i] = new Label("0");
            factionStrengthLabels[i] = new Label("-");
            factionCombatLabels[i] = new Label("-");
            factionMaxExpLabels[i] = new Label("-");
            factionAvgExpLabels[i] = new Label("-");
            factionDeathsLabels[i] = new Label("0");

            factionTable.add(new Label(FACTIONS[i].getName()), 0, i + 1);
            factionTable.add(factionTotalLabels[i], 1, i + 1);
            factionTable.add(factionMaleLabels[i], 2, i + 1);
            factionTable.add(factionFemaleLabels[i], 3, i + 1);
            factionTable.add(factionStrengthLabels[i], 4, i + 1);
            factionTable.add(factionCombatLabels[i], 5, i + 1);
            factionTable.add(factionMaxExpLabels[i], 6, i + 1);
            factionTable.add(factionAvgExpLabels[i], 7, i + 1);
            factionTable.add(factionDeathsLabels[i], 8, i + 1);
        }

        VBox generalStats = new VBox(2);
        generalStats.getChildren().addAll(
                populationLabel,
                maleCountLabel,
                femaleCountLabel,
                avgAgeLabel,
                avgFertilityLabel
        );

        this.statsPanel = new HBox(30);
        this.statsPanel.setPadding(new Insets(10));
        this.statsPanel.setStyle("-fx-background-color: #e8e8e8;");
        this.statsPanel.getChildren().addAll(generalStats, factionTable);

        // Игровое поле
        this.canvas = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.canvas.setOnMousePressed(this::handlerDraw);
        this.canvas.setOnMouseDragged(this::handlerDraw);

        this.setOnKeyPressed(this::onKeyPressed);

        int gridWidth = WINDOW_WIDTH / CELL_SIZE;
        int gridHeight = WINDOW_HEIGHT / CELL_SIZE;
        this.simulation = new Simulation(gridWidth, gridHeight);

        this.affine = new Affine();
        this.affine.appendScale(CELL_SIZE, CELL_SIZE);

        // Компоновка
        this.setTop(controls);
        this.setCenter(canvas);
        this.setRight(infoPanel);
        this.setBottom(statsPanel);

        this.widthProperty().addListener((obs, oldVal, newVal) -> onResize());
        this.heightProperty().addListener((obs, oldVal, newVal) -> onResize());

        // Ресайз после полной инициализации сцены
        Platform.runLater(this::onResize);

        this.gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (running) {
                    // Делаем несколько шагов если прошло много времени
                    long elapsed = now - lastUpdate;
                    int steps = (int) (elapsed / updateInterval);
                    if (steps > 0) {
                        // Ограничиваем максимум шагов за кадр
                        steps = Math.min(steps, 10);
                        for (int i = 0; i < steps; i++) {
                            simulation.step();
                        }
                        lastUpdate = now;
                        updateCellInfo();
                        updateStats();
                    }
                }
                draw();
            }
        };
        gameLoop.start();
    }

    private void onKeyPressed(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.D) {
            this.drawMode = 1;
        } else if (keyEvent.getCode() == KeyCode.E) {
            this.drawMode = 0;
        }
    }

    private void handlerDraw(MouseEvent mouseEvent) {
        double mouseX = mouseEvent.getX();
        double mouseY = mouseEvent.getY();

        try {
            Point2D simCoords = this.affine.inverseTransform(mouseX, mouseY);
            int x = (int) simCoords.getX();
            int y = (int) simCoords.getY();

            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                // ПКМ — выбор клетки для просмотра
                selectCell(x, y);
            } else if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                if (this.drawMode == 1) {
                    Faction selectedFaction = factionComboBox.getValue();
                    String sexChoice = sexComboBox.getValue();
                    com.jeto.game.points.Sex sex = null;
                    if ("M".equals(sexChoice)) {
                        sex = com.jeto.game.points.Sex.MALE;
                    } else if ("F".equals(sexChoice)) {
                        sex = com.jeto.game.points.Sex.FEMALE;
                    }
                    simulation.setAlive(x, y, selectedFaction, sex);
                } else {
                    // В режиме E — выбор клетки ЛКМ
                    selectCell(x, y);
                }
            }
        } catch (NonInvertibleTransformException e) {
            System.out.println("Не могу перевести коорды");
        }
    }

    private void selectCell(int x, int y) {
        this.selectedX = x;
        this.selectedY = y;
        updateCellInfo();
    }

    private void updateCellInfo() {
        if (selectedX < 0 || selectedY < 0) {
            return;
        }

        Point point = simulation.getPoint(selectedX, selectedY);
        coordsLabel.setText("Position: " + selectedX + ", " + selectedY);
        aliveLabel.setText("State: " + (point.isAlive() ? "Alive" : "Dead"));
        idLabel.setText("ID: " + point.getId());
        factionLabel.setText("Faction: " + (point.getFaction() != null ? point.getFaction().getName() : "-"));
        sexLabel.setText("Sex: " + (point.getSex() != null ? point.getSex().name() : "-"));
        ageLabel.setText("Age: " + point.getAge());

        // Strength, Combat Power и Exp
        strengthLabel.setText("Strength: " + String.format("%.0f", point.getStrength()));
        double combat = point.getCombatPower() * 100;
        combatPowerLabel.setText("Combat: " + (combat > 0 ? String.format("%.0f", combat) : "-"));
        int exp = point.getCombatExp();
        combatExpLabel.setText("Exp: " + (exp > 0 ? exp : "-"));

        // Fertility в процентах
        double fertility = point.getFertility() * 100;
        fertilityLabel.setText("Fertility: " + (fertility > 0 ? String.format("%.0f%%", fertility) : "-"));

        // Move chance
        double moveChance = point.getMoveChance() * 100;
        moveChanceLabel.setText("Move chance: " + String.format("%.0f%%", moveChance));

        // Birth cooldown
        int cooldown = point.getBirthCooldown();
        birthCooldownLabel.setText("Birth cooldown: " + (cooldown > 0 ? cooldown : "-"));

        // Родители
        long motherId = point.getMotherId();
        long fatherId = point.getFatherId();
        motherIdLabel.setText("Mother ID: " + (motherId > 0 ? motherId : "-"));
        fatherIdLabel.setText("Father ID: " + (fatherId > 0 ? fatherId : "-"));
    }

    private void updateStats() {
        int population = 0;
        int males = 0;
        int females = 0;
        int totalAge = 0;
        double totalFertility = 0;
        int fertileCount = 0;
        int[] factionTotals = new int[FACTIONS.length];
        int[] factionMales = new int[FACTIONS.length];
        int[] factionFemales = new int[FACTIONS.length];
        double[] factionStrengthSum = new double[FACTIONS.length];
        double[] factionCombatSum = new double[FACTIONS.length];
        int[] factionFighters = new int[FACTIONS.length];
        int[] factionMaxExp = new int[FACTIONS.length];
        int[] factionTotalExp = new int[FACTIONS.length];

        int simWidth = simulation.getWidth();
        int simHeight = simulation.getHeight();

        for (int x = 0; x < simWidth; x++) {
            for (int y = 0; y < simHeight; y++) {
                Point point = simulation.getPoint(x, y);
                if (point.isAlive()) {
                    population++;
                    totalAge += point.getAge();

                    boolean isMale = point.getSex() == com.jeto.game.points.Sex.MALE;
                    if (isMale) {
                        males++;
                    } else {
                        females++;
                    }

                    double fertility = point.getFertility();
                    if (fertility > 0) {
                        totalFertility += fertility;
                        fertileCount++;
                    }

                    // Faction count
                    Faction faction = point.getFaction();
                    if (faction != null) {
                        int fid = faction.getId();
                        factionTotals[fid]++;
                        factionStrengthSum[fid] += point.getStrength();
                        if (isMale) {
                            factionMales[fid]++;
                            if (point.canFight()) {
                                factionCombatSum[fid] += point.getCombatPower();
                                factionFighters[fid]++;
                                int exp = point.getCombatExp();
                                factionTotalExp[fid] += exp;
                                if (exp > factionMaxExp[fid]) {
                                    factionMaxExp[fid] = exp;
                                }
                            }
                        } else {
                            factionFemales[fid]++;
                        }
                    }
                }
            }
        }

        populationLabel.setText("Population: " + population);
        maleCountLabel.setText("Males: " + males);
        femaleCountLabel.setText("Females: " + females);

        double avgAge = population > 0 ? (double) totalAge / population : 0;
        avgAgeLabel.setText("Avg Age: " + String.format("%.1f", avgAge));

        double avgFertility = fertileCount > 0 ? (totalFertility / fertileCount) * 100 : 0;
        avgFertilityLabel.setText("Avg Fertility: " + String.format("%.0f%%", avgFertility));

        // Update faction table
        for (int i = 0; i < FACTIONS.length; i++) {
            factionTotalLabels[i].setText(String.valueOf(factionTotals[i]));
            factionMaleLabels[i].setText(String.valueOf(factionMales[i]));
            factionFemaleLabels[i].setText(String.valueOf(factionFemales[i]));

            double avgStr = factionTotals[i] > 0 ? factionStrengthSum[i] / factionTotals[i] : 0;
            factionStrengthLabels[i].setText(factionTotals[i] > 0 ? String.format("%.0f", avgStr) : "-");

            double avgCombat = factionFighters[i] > 0 ? (factionCombatSum[i] / factionFighters[i]) * 100 : 0;
            factionCombatLabels[i].setText(factionFighters[i] > 0 ? String.format("%.0f", avgCombat) : "-");

            factionMaxExpLabels[i].setText(factionFighters[i] > 0 ? String.valueOf(factionMaxExp[i]) : "-");
            double avgExp = factionFighters[i] > 0 ? (double) factionTotalExp[i] / factionFighters[i] : 0;
            factionAvgExpLabels[i].setText(factionFighters[i] > 0 ? String.format("%.1f", avgExp) : "-");

            factionDeathsLabels[i].setText(String.valueOf(FACTIONS[i].getDeaths()));
        }
    }

    public void draw() {
        int simWidth = this.simulation.getWidth();
        int simHeight = this.simulation.getHeight();

        GraphicsContext g = this.canvas.getGraphicsContext2D();
        g.setTransform(this.affine);
        g.setFill(Color.LIGHTGRAY);
        g.fillRect(0, 0, simWidth, simHeight);

        for (int x = 0; x < simWidth; x++) {
            for (int y = 0; y < simHeight; y++) {
                Point point = this.simulation.getPoint(x, y);
                Color color = point.getColor();
                if (color != null) {
                    g.setFill(color);
                    g.fillRect(x, y, 1, 1);
                }
            }
        }

        g.setStroke(Color.BLACK);
        g.setLineWidth(0.05d);
        for (int x = 0; x <= simWidth; x++) {
            g.strokeLine(x, 0, x, simHeight);
        }

        for (int y = 0; y <= simHeight; y++) {
            g.strokeLine(0, y, simWidth, y);
        }
    }

    private void onResize() {
        double controlsHeight = controls.getHeight();
        double statsHeight = statsPanel.getHeight() > 0 ? statsPanel.getHeight() : 40;
        double infoPanelWidth = infoPanel.getPrefWidth();
        double availableWidth = getWidth() - infoPanelWidth;
        double availableHeight = getHeight() - controlsHeight - statsHeight;

        int newGridWidth = (int) (availableWidth / CELL_SIZE);
        int newGridHeight = (int) (availableHeight / CELL_SIZE);

        if (newGridWidth > 0 && newGridHeight > 0) {
            this.simulation.resize(newGridWidth, newGridHeight);
            this.canvas.setWidth(newGridWidth * CELL_SIZE);
            this.canvas.setHeight(newGridHeight * CELL_SIZE);
        }
    }
}
