package com.omokpang;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 역할: 전역 화면 전환 유틸리티(싱글톤).
 * 핵심기능:
 *  - FXML 경로만으로 Scene 전환
 *  - 크기/타이틀 기본값 유지
 */
public final class SceneRouter {
    private static Stage stage;

    private SceneRouter() {}

    public static void init(Stage primaryStage) {
        stage = primaryStage;
    }

    /** fxmlPath 예: "/fxml/auth/AuthView.fxml" */
    public static void go(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneRouter.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, App.APP_WIDTH, App.APP_HEIGHT); // ← 크기 강제

            stage.setScene(scene);
            stage.setWidth(App.APP_WIDTH);
            stage.setHeight(App.APP_HEIGHT);
            stage.setMinWidth(App.APP_WIDTH);
            stage.setMinHeight(App.APP_HEIGHT);
            stage.setMaxWidth(App.APP_WIDTH);
            stage.setMaxHeight(App.APP_HEIGHT);

            stage.setResizable(false);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
