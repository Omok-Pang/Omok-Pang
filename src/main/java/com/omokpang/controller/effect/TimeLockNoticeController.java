package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 역할: 상대에게 Time Lock 카드가 적용됐을 때
 *      화면 아래쪽에 안내 문구를 잠깐 보여주는 오버레이.
 *
 * - GameBoardController에서 FXML을 로드해서 centerStack 위에 올려주면,
 *   여기서 2초 정도 보여준 뒤 자동으로 StackPane에서 제거한다.
 * - 클릭은 모두 보드로 통과되도록 mouseTransparent 처리.
 */
public class TimeLockNoticeController {

    @FXML private StackPane rootOverlay;
    @FXML private Label noticeLabel;

    @FXML
    public void initialize() {
        if (noticeLabel != null &&
                (noticeLabel.getText() == null || noticeLabel.getText().isBlank())) {
            noticeLabel.setText(
                    "상대방이 Time Lock 카드를 사용했습니다.\n" +
                            "당신의 제한시간이 3초로 줄어듭니다."
            );
        }

        // 안내만 띄우는 용도이므로 클릭은 보드로 통과
        if (rootOverlay != null) {
            rootOverlay.setMouseTransparent(true);
        }

        // 2초 후 자동 제거
        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> close());
        pause.play();
    }

    /** 오버레이 제거 (centerStack에서 제거) */
    private void close() {
        if (rootOverlay == null) return;

        if (rootOverlay.getParent() instanceof StackPane parent) {
            parent.getChildren().remove(rootOverlay);
        }
    }
}