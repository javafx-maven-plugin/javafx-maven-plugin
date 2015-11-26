package com.zenjava.test;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SecondaryMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setScene(new Scene(new Label("Hello World 2!")));
        primaryStage.show();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

}
