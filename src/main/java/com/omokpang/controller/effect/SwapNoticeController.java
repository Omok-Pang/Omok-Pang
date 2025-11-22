package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 상대가 Swap 카드를 사용했을 때,
 * 중앙에 "돌의 위치가 교환되었습니다" 안내를 잠깐 보여주는 오버레이.
 */
public class SwapNoticeController {

    @FXML private StackPane rootOverlay;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        if (messageLabel != null &&
                (messageLabel.getText() == null || messageLabel.getText().isBlank())) {
            messageLabel.setText("상대방이 공격카드를 사용했습니다. 돌의 위치가 교환되었습니다.");
        }

        if (rootOverlay != null) {
            rootOverlay.setMouseTransparent(false); // 아래 클릭 잠깐 막기
        }

        // 2초 후 자동 제거
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> close());
        pause.play();
    }

    private void close() {
        if (rootOverlay == null) return;
        if (rootOverlay.getParent() instanceof StackPane parent) {
            parent.getChildren().remove(rootOverlay);
        }
    }
}
