package com.omokpang;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Hello, OmokPang!");
        Scene scene = new Scene(label, 750, 600);
        stage.setScene(scene);
        stage.setTitle("OmokPang Test");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
