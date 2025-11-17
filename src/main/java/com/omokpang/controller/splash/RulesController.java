/** RulesModalController
 * 역할: 게임 규칙 모달 팝업.
 * 핵심기능: 규칙 텍스트 표시 / 확인 버튼으로 모달 닫기.
 */

package com.omokpang.controller.splash;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class RulesController {

    @FXML
    private Button confirmButton; // fxml 버튼 연결용 (선택사항)

    // ✅ "확인" 버튼 클릭 시 AuthView.fxml로 전환
    @FXML
    private void handleConfirm() {
        try {
            FXMLLoader loader = new FXMLLoader( getClass().getResource("/fxml/game/GameIntroView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) confirmButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Omok Pang - 로그인");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
