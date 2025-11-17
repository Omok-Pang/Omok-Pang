/** CardUseModalController
 * 역할: 보유 카드 중 사용할 카드 선택(필요 시 타겟 수집).
 * 핵심기능: 손패 렌더링 / 선택 확정 콜백·전송.
 */

package com.omokpang.controller.game;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class CardUseModalController {

    @FXML
    private StackPane root; // FXML의 fx:id="root"
    private Runnable onCardSelected;

    // ───────────────── 카드 선택 시 ─────────────────
    public void setOnCardSelected(Runnable onCardSelected) {
        this.onCardSelected = onCardSelected;
    }

    @FXML
    private void handleSelectCard() {
        if (onCardSelected != null) {
            onCardSelected.run();   // ★ GameBoard로 "카드 사용" 알림
        }
        close();
    }

    // 닫기 버튼 클릭 시
    @FXML
    private void handleClose() {
        close();
    }

    // ───────────────── 실제 닫기 로직 ─────────────────
    private void close() {
        if (root == null) return;

        if (root.getParent() instanceof Pane parent) {
            parent.getChildren().remove(root);
        }
    }
}
