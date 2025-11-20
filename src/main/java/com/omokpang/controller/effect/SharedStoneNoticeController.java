package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** SharedStone 카드 사용을 상대에게 보여주는 안내 */
public class SharedStoneNoticeController {

    @FXML private StackPane rootOverlay;
    @FXML private Label noticeLabel;

    @FXML
    public void initialize() {
        noticeLabel.setText(
                "상대방이 Shared Stone 카드를 사용했습니다.\n" +
                        "당신의 돌 1개가 상대방과 공유됩니다."
        );

        // 안내만 띄우고, 클릭은 보드로 통과
        rootOverlay.setMouseTransparent(true);

        // 3초 후 자동 제거
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
