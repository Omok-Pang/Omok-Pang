package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class RemoveNoticeController {

    @FXML private StackPane root;
    @FXML private Label titleLabel;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        // 2.5초 뒤 자동으로 사라지게
        PauseTransition hide = new PauseTransition(Duration.seconds(2.5));
        hide.setOnFinished(e -> {
            if (root.getParent() instanceof javafx.scene.layout.Pane parent) {
                parent.getChildren().remove(root);
            }
        });
        hide.play();
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}