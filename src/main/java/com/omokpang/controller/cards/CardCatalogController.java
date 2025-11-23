/** CardCatalogController : 카드 종류 안내 모달.
 * 역할: 8종 카드 설명을 표시하고 모달을 닫는 기능만 담당.
 * CardSelectController에서 "카드 종류" 버튼으로 호출됨.
 */

package com.omokpang.controller.cards;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.Button;

public class CardCatalogController {

    @FXML private Button closeBtn;

    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeBtn.getScene().getWindow();
        stage.close();
    }
}