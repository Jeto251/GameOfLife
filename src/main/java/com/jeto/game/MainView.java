package com.jeto.game;

import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.Timer;
import java.util.TimerTask;

public class MainView extends VBox {

   private Button stepButton;
   private Canvas canvas;
   private int width = 400;
   private int height = 400;

   private Affine affine;
   private Simulation simulation;

   private int drawMode = 0;

   public MainView() {
	  this.stepButton = new Button("Start");
	  this.stepButton.setOnAction(actionEvent -> {
		 new Timer().scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
			   stepAndDraw();
			}
		 },0,200);
	  });

	  this.canvas = new Canvas(width, height);
	  this.canvas.setOnMousePressed(this::handlerDraw);
	  this.canvas.setOnMouseDragged(this::handlerDraw);

	  this.setOnKeyPressed(this::onKeyPressed);

	  this.simulation = new Simulation(50, 50);

	  this.affine = new Affine();
	  this.affine.appendScale(
		   width / (float)simulation.getWidth(),
		   height / (float)simulation.getHeight()
	  );

	  this.getChildren().addAll(this.stepButton, this.canvas);
   }

   public void stepAndDraw() {
	  simulation.step();
	  draw();
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
			draw();
		 }
	  } catch (NonInvertibleTransformException e) {
		 System.out.println("Не могу перевести коорды");
	  }
   }

   public void draw() {
	  GraphicsContext g = this.canvas.getGraphicsContext2D();
	  g.setTransform(this.affine);
	  g.setFill(Color.LIGHTGRAY);
	  g.fillRect(0, 0, width, height);

	  g.setFill(Color.color(0.4,0.6,0.6));
	  for (int x = 0; x < this.simulation.getWidth(); x++) {
		 for (int y = 0; y < this.simulation.getHeight(); y++) {
			if (!this.simulation.isDead(x, y)) {
			   g.fillRect(x, y, 1, 1);
			}
		 }
	  }

	  g.setStroke(Color.BLACK);
	  g.setLineWidth(0.05d);
	  for (int x = 0; x <= this.simulation.getWidth(); x++) {
		 g.strokeLine(x, 0, x, simulation.getWidth());
	  }

	  for (int y = 0; y <= this.simulation.getHeight(); y++) {
		 g.strokeLine(0, y, simulation.getHeight(), y);
	  }
   }
}
