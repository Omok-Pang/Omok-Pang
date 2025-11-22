package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Defense 사용 시 하단 안내 배너.
 * 2~3초 후 자동으로 사라짐.
 */
public class DefenseNoticeController {

    @FXML private StackPane root;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {

        // 2.5초 뒤 자동으로 사라지도록 설정
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> {
            if (root != null && root.getParent() != null) {
                ((StackPane) root.getParent()).getChildren().remove(root);
            }
        });
        delay.play();
    }

    public void setTexts(String title, String message) {
        if (titleLabel != null) titleLabel.setText(title);
        if (messageLabel != null) messageLabel.setText(message);
    }
}
