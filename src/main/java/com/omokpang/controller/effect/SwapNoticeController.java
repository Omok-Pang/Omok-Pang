package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 역할: Swap 결과 안내 오버레이를 일정 시간 보여주고 자동으로 닫은 뒤 콜백 실행
 */
public class SwapNoticeController {

    @FXML
    private StackPane root;   // fx:id="root"

    /**
     * @param parent    centerStack 같은 부모 컨테이너
     * @param onFinished 2초 후 오버레이 제거가 끝나고 실행할 콜백 (없으면 null)
     */
    public void showOn(StackPane parent, Runnable onFinished) {
        parent.getChildren().add(root);

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> {
            if (root.getParent() instanceof Pane p) {
                p.getChildren().remove(root);
            }
            if (onFinished != null) {
                onFinished.run();
            }
        });
        pause.play();
    }
}
