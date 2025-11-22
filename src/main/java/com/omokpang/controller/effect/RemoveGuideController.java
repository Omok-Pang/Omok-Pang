package com.omokpang.controller.effect;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * 역할: Remove 카드 사용 시,
 *  - "삭제를 원하는 상대방의 돌을 선택해주세요." 안내 배너를
 *    화면 하단에 띄우는 가이드 컨트롤러.
 *  - 좌표 선택은 GameBoardController에서 처리하고,
 *    여기서는 텍스트만 보여준다.
 */
public class RemoveGuideController {

    @FunctionalInterface
    public interface OnStoneSelected {
        void onStoneSelected(int row, int col);
    }

    private OnStoneSelected listener;

    @FXML
    private StackPane root;   // RemoveGuide.fxml 최상단 컨테이너

    @FXML
    private Label guideLabel; // 하단 문구 표시 라벨

    @FXML
    public void initialize() {
        if (guideLabel != null) {
            guideLabel.setText("삭제를 원하는 상대방의 돌을 선택해주세요.");
        }
    }

    /** GameBoardController에서 콜백 등록 */
    public void setOnStoneSelected(OnStoneSelected listener) {
        this.listener = listener;
    }

    /** GameBoardController가 보드 클릭 좌표를 알려줄 때 호출 */
    public void notifyStoneSelected(int row, int col) {
        if (listener != null) {
            listener.onStoneSelected(row, col);
        }
    }

    /** FXML에서 닫기 버튼을 눌렀을 때 (필요하면 사용) */
    @FXML
    private void handleClose() {
        close();
    }

    /** 오버레이 제거 */
    public void close() {
        if (root != null && root.getParent() instanceof Pane parent) {
            parent.getChildren().remove(root);
        }
    }
}
