package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class DoubleMoveNoticeController {

    @FXML private StackPane root;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        // 기본 문구 (setMessage로 덮어쓸 수 있음)
        if (messageLabel != null &&
                (messageLabel.getText() == null || messageLabel.getText().isBlank())) {
            messageLabel.setText("두 번 두기 카드가 사용되었습니다.");
        }

        // 2.5초 후 자동으로 화면에서 사라지게
        PauseTransition pt = new PauseTransition(Duration.seconds(2.5));
        pt.setOnFinished(e -> close());
        pt.play();
    }

    /** GameBoardController 쪽에서 커스텀 문구를 넣을 때 사용 */
    public void setMessage(String text) {
        if (messageLabel != null) {
            messageLabel.setText(text);
        }
    }

    /** 자기 자신을 부모 StackPane에서 제거 */
    private void close() {
        if (root == null) return;
        if (root.getParent() instanceof Pane parent) {
            parent.getChildren().remove(root);
        }
    }
}
