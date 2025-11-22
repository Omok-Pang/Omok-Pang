package com.omokpang.controller.effect;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.function.BiConsumer;

/**
 * Bomb!! 카드를 사용하는 유저가 보는 안내 화면.
 * - SharedStone 가이드처럼 "마우스 이벤트 투과" 되어야 함.
 * - 보드 클릭을 막으면 안됨.
 */
public class BombGuideController {

    @FXML private StackPane rootOverlay;
    @FXML private Label guideLabel;

    private BiConsumer<Integer, Integer> onAreaSelected;

    @FXML
    public void initialize() {
        if (guideLabel != null && (guideLabel.getText() == null || guideLabel.getText().isBlank())) {
            guideLabel.setText("Bomb!! 카드를 선택했습니다.\n돌 한개를 선택하면 그 주변 3x3에 위치한 돌도 제거됩니다.");
        }
        rootOverlay.setMouseTransparent(true);
    }

    public void setOnAreaSelected(BiConsumer<Integer, Integer> callback) {
        this.onAreaSelected = callback;
    }

    /** GameBoard에서 보드 클릭 시 호출 */
    public void notifyAreaSelected(int r, int c) {
        if (onAreaSelected != null) {
            onAreaSelected.accept(r, c);
        }
        closeOverlay();
    }

    private void closeOverlay() {
        rootOverlay.setVisible(false);
        rootOverlay.setManaged(false);

        if (rootOverlay.getParent() instanceof StackPane parent) {
            parent.getChildren().remove(rootOverlay);
        }
    }
}
