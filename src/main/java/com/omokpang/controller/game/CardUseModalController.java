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

    // 카드 선택 시 (지금은 단순히 모달 닫기만 수행)
    @FXML
    private void handleSelectCard() {
        close();
        // TODO: 여기서 선택된 카드 정보를 GameBoard 쪽으로 전달하는 로직을 추후 추가
    }

    // 닫기 버튼 클릭 시
    @FXML
    private void handleClose() {
        close();
    }

    /**
     * 현재 모달을 부모 컨테이너(예: centerStack)에서 제거하여 닫는다.
     */
    private void close() {
        if (root == null) return;

        if (root.getParent() instanceof Pane parent) {
            parent.getChildren().remove(root);
        }
    }
}
