/** CardCatalogController
 * 역할: 카드 종류 안내 모달.
 * 핵심기능: 8종 카드 메타 표시 / 닫기.
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
