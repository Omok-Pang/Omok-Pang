package com.omokpang.controller.effect;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.function.BiConsumer;

/** SharedStone 카드 사용자가 보는 안내 화면 */
public class SharedStoneGuideController {

    @FXML private StackPane rootOverlay;
    @FXML private Label guideLabel;

    // GameBoardController로 r,c 좌표를 전달하는 콜백
    private BiConsumer<Integer, Integer> onStoneSelected;

    @FXML
    public void initialize() {
        guideLabel.setText(
                "Shared Stone 카드를 선택했습니다.\n" +
                        "공용돌로 만들 상대방 돌 1개를 선택해주세요."
        );

        // 안내용이라 마우스 이벤트는 통과시켜도 됨
        rootOverlay.setMouseTransparent(true);
    }

    /** GameBoard가 콜백 등록 */
    public void setOnStoneSelected(BiConsumer<Integer, Integer> callback) {
        this.onStoneSelected = callback;
    }

    /** GameBoard에서 호출해주는 API → 사용자가 상대 돌 클릭했을 때 */
    public void notifyStoneSelected(int r, int c) {
        if (onStoneSelected != null) {
            onStoneSelected.accept(r, c);
        }
        closeOverlay();
    }

    /** GameBoardController 외부에서 닫을 수 있도록 public close() 제공 */
    public void close() {
        closeOverlay();
    }

    /** 오버레이 제거 */
    private void closeOverlay() {
        rootOverlay.setVisible(false);
        rootOverlay.setManaged(false);

        if (rootOverlay.getParent() instanceof StackPane parent) {
            parent.getChildren().remove(rootOverlay);
        }
    }
}
