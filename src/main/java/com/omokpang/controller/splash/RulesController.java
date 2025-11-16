/** RulesModalController
 * 역할: 게임 규칙 모달 팝업.
 * 핵심기능: 규칙 텍스트 표시 / 확인 버튼으로 모달 닫기.
 */

package com.omokpang.controller.splash;

import com.omokpang.controller.main.MainController;
import com.omokpang.domain.user.User;
import com.omokpang.session.AppSession;
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
    // + 로그인 상태면 MainView로 전환
    @FXML
    private void handleConfirm() {
        try {
            Stage stage = (Stage) confirmButton.getScene().getWindow();

            if (AppSession.isLoggedIn()) {
                // ---- 로그인 상태: MainView로 복귀 + 유저 정보 다시 세팅 ----
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/main/MainView.fxml"));
                Parent root = loader.load();

                MainController controller = loader.getController();
                User user = AppSession.getCurrentUser();
                if (user != null) {
                    controller.setUserInfo(user.getNickname(), "/images/user/ic_profile.png");
                    controller.setStats(user.getPoints(), user.getWins());
                }

                stage.setScene(new Scene(root));
                stage.setTitle("OmokPang - 메인 화면");
                stage.show();

            } else {
                // ---- 비로그인 상태: 로그인 화면으로 이동 ----
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/auth/AuthView.fxml"));
                Parent root = loader.load();

                stage.setScene(new Scene(root));
                stage.setTitle("Omok Pang - 로그인");
                stage.show();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}