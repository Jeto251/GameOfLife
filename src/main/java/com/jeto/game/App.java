package com.jeto.game;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import static com.jeto.game.Config.*;

import javafx.stage.WindowEvent;

import java.io.IOException;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setTitle("Game");
        stage.setScene(scene);
        stage.show();

        mainView.draw();
    }

    public static void main(String[] args) {
        launch();
    }
}