package com.omokpang.controller.effect;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

/**
 * 역할: Swap 선택 안내 오버레이를 띄우고/닫는 역할
 *  - 자동으로 사라지지는 않음 (사용자가 선택을 마치면 GameBoard 쪽에서 close() 호출 예정)
 */
public class SwapSelectGuideController {

    @FXML
    private StackPane root;   // fx:id="root"

    /** centerStack 위에 오버레이를 올린다. */
    public void showOn(StackPane parent) {
        parent.getChildren().add(root);
    }

    /** GameBoard 쪽에서 선택이 끝났을 때 호출해서 오버레이 제거 */
    public void close() {
        if (root == null) return;
        if (root.getParent() instanceof Pane parent) {
            parent.getChildren().remove(root);
        }
    }
}
