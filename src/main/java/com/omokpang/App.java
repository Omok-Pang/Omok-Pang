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
    public static final double APP_WIDTH = 750;
    public static final double APP_HEIGHT = 600;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1) 최초 화면: SplashView.fxml 로드
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/cards/CardSelectView.fxml"));

        // 2) Scene 생성 및 Stage 세팅
        Scene scene = new Scene(root);
        // 필요하면 스타일시트: scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setTitle("OmokPang");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false); // 고정 크기
        primaryStage.show();

        // 3) 전역 라우터 초기화(다른 컨트롤러에서 화면 전환 쉽게 사용)
        SceneRouter.init(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
