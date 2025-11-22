package com.omokpang;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * 역할: JavaFX 앱 시작점.
 * 핵심기능:
 *  - SplashView.fxml 로드하여 첫 화면 표시
 *  - SceneRouter 초기화(전역 화면 전환용)
 *  - 윈도우 공통 옵션(title/size/resize 정책) 세팅
 */
public class App extends Application {

    // 앱 전역 기본 크기(디자인 기준)
    public static final double APP_WIDTH = 800;
    public static final double APP_HEIGHT = 800;

    @Override
    public void start(Stage primaryStage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Splash/SplashView.fxml"));
        Scene scene = new Scene(root, APP_WIDTH, APP_HEIGHT);

        primaryStage.setTitle("OmokPang");
        primaryStage.setScene(scene);

        // 🔥 윈도우 크기 강제 고정
        primaryStage.setWidth(APP_WIDTH);
        primaryStage.setHeight(APP_HEIGHT);
        primaryStage.setMinWidth(APP_WIDTH);
        primaryStage.setMinHeight(APP_HEIGHT);
        primaryStage.setMaxWidth(APP_WIDTH);
        primaryStage.setMaxHeight(APP_HEIGHT);

        primaryStage.setResizable(false);
        primaryStage.show();

        SceneRouter.init(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
