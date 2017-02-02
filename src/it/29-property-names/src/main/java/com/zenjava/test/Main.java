package com.zenjava.test;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    @Override
    public void start(Stage primaryStage) throws Exception {
        LOGGER.log(Level.INFO, "Starting JavaFX application...");
        primaryStage.setScene(new Scene(new Label("Hello World!")));
        primaryStage.show();
        LOGGER.log(Level.INFO, "Started JavaFX application!");
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
