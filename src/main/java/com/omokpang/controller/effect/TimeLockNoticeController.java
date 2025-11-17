package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 역할: Time Lock 안내 오버레이를 일정 시간 보여주고 자동으로 닫은 뒤 콜백 실행
 */
public class TimeLockNoticeController {

    @FXML
    private StackPane root;   // fx:id="root"

    private StackPane parent; // 얹힐 컨테이너(centerStack)
    private Runnable onFinished;

    /**
     * GameBoard 쪽에서 호출해서 오버레이를 띄운다.
     * @param parent    centerStack 같은 부모 컨테이너
     * @param onFinished 2초 후 오버레이 제거가 끝나고 실행할 콜백 (예: 3초 타이머 시작)
     */
    public void showOn(StackPane parent, Runnable onFinished) {
        this.parent = parent;
        this.onFinished = onFinished;

        parent.getChildren().add(root);

        // 2초 후 자동으로 제거 + 콜백 실행
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            parent.getChildren().remove(root);
            if (this.onFinished != null) {
                this.onFinished.run();
            }
        });
        pause.play();
    }
}
