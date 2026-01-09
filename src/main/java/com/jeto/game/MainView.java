package com.jeto.game;

import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import static com.jeto.game.Config.*;

public class MainView extends BorderPane {

    private Button stepButton;
    private Canvas canvas;
    private HBox controls;

    private Affine affine;
    private Simulation simulation;

    private int drawMode = 0;

    private AnimationTimer gameLoop;
    private boolean running = false;
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 200_000_000; // 200ms в наносекундах

    public MainView() {
        // Панель управления
        this.stepButton = new Button("Start");
        this.stepButton.setOnAction(e -> {
            running = !running;
            stepButton.setText(running ? "Stop" : "Start");
        });

        this.controls = new HBox(10);
        this.controls.setPadding(new Insets(10));
        this.controls.setStyle("-fx-background-color: #e0e0e0;");
        this.controls.getChildren().addAll(stepButton);

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

        this.widthProperty().addListener((obs, oldVal, newVal) -> onResize());
        this.heightProperty().addListener((obs, oldVal, newVal) -> onResize());

        onResize();

        this.gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= UPDATE_INTERVAL) {
                    if (running) {
                        simulation.step();
                    }
                    lastUpdate = now;
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

            if (this.drawMode == 1) {
                simulation.setAlive(x, y);
            }
        } catch (NonInvertibleTransformException e) {
            System.out.println("Не могу перевести коорды");
        }
    }

    public void draw() {
        int simWidth = this.simulation.getWidth();
        int simHeight = this.simulation.getHeight();

        GraphicsContext g = this.canvas.getGraphicsContext2D();
        g.setTransform(this.affine);
        g.setFill(Color.LIGHTGRAY);
        g.fillRect(0, 0, simWidth, simHeight);

        g.setFill(Color.color(0.4, 0.6, 0.6));
        for (int x = 0; x < simWidth; x++) {
            for (int y = 0; y < simHeight; y++) {
                if (!this.simulation.isDead(x, y)) {
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
        double availableHeight = getHeight() - controlsHeight;

        int newGridWidth = (int) (getWidth() / CELL_SIZE);
        int newGridHeight = (int) (availableHeight / CELL_SIZE);

        if (newGridWidth > 0 && newGridHeight > 0) {
            this.simulation.resize(newGridWidth, newGridHeight);
            this.canvas.setWidth(newGridWidth * CELL_SIZE);
            this.canvas.setHeight(newGridHeight * CELL_SIZE);
        }
    }
}
