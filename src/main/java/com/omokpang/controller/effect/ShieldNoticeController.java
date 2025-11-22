package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class ShieldNoticeController {

    @FXML private StackPane root;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;

    public void setTexts(String title, String message) {
        if (titleLabel != null && title != null) {
            titleLabel.setText(title);
        }
        if (messageLabel != null && message != null) {
            messageLabel.setText(message);
        }
    }

    @FXML
    public void initialize() {
        // 2초 후 자동으로 사라지도록
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> close());
        delay.play();
    }

    public void close() {
        if (root != null && root.getParent() instanceof Pane parent) {
            parent.getChildren().remove(root);
        }
    }
}
