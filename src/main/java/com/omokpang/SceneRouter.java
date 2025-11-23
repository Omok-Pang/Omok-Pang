package com.omokpang;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * SceneRouter
 * 역할: 전역 화면 전환을 담당하는 유틸리티(싱글톤).
 * 핵심기능:
 *  - FXML 경로만 전달하면 바로 화면 전환
 *  - App.APP_WIDTH/HEIGHT 기반으로 크기 고정
 *  - Stage를 공유해 전체 앱 내 Scene 일원화
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
            Scene scene = new Scene(root, App.APP_WIDTH, App.APP_HEIGHT);

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
