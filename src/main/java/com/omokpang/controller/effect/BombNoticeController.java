package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 상대방이 Bomb!! 카드를 사용했을 때 보여주는 안내.
 * - 클릭은 보드에 전달되도록 투과 처리.
 */
public class BombNoticeController {

    @FXML private StackPane rootOverlay;
    @FXML private Label noticeLabel;

    @FXML
    public void initialize() {
        if (noticeLabel != null) {
            noticeLabel.setText(
                    "상대방이 Bomb!! 카드를 사용했습니다.\n" +
                            "상대방이 선택한 3×3 구역이 제거됩니다."
            );
        }

        rootOverlay.setMouseTransparent(true);

        PauseTransition pt = new PauseTransition(Duration.seconds(3));
        pt.setOnFinished(e -> close());
        pt.play();
    }

    private void close() {
        if (rootOverlay.getParent() instanceof StackPane parent) {
            parent.getChildren().remove(rootOverlay);
        }
    }
}
