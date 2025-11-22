package com.omokpang.controller.effect;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Swap 카드 사용 시, 하단에
 * "자신의 돌 1개와 상대방 돌 1개를 선택하세요" 안내를 보여주는 오버레이.
 * - 첫 클릭(내 돌) 이후에는 문구를 바꿔주고
 * - GameBoard에서 두 번째 선택이 끝나면 close()를 호출해 제거한다.
 */
public class SwapSelectGuideController {

    @FXML private StackPane rootOverlay;
    @FXML private Label guideLabel;

    @FXML
    public void initialize() {
        if (guideLabel != null &&
                (guideLabel.getText() == null || guideLabel.getText().isBlank())) {
            guideLabel.setText("변경하고 싶은 자신의 돌 1개와 상대방 돌 1개를 선택하세요");
        }
        if (rootOverlay != null) {
            rootOverlay.setMouseTransparent(true); // 클릭은 보드로 통과
        }
    }

    /** 내 돌을 선택한 뒤 두 번째 단계 안내로 문구 변경 */
    public void onMyStoneSelected() {
        if (guideLabel != null) {
            guideLabel.setText("이제 교환할 상대방의 돌 1개를 선택하세요");
        }
    }

    /** 오버레이 제거 */
    public void close() {
        if (rootOverlay == null) return;
        if (rootOverlay.getParent() instanceof StackPane parent) {
            parent.getChildren().remove(rootOverlay);
        }
    }
}
