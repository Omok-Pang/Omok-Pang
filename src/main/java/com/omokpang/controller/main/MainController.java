/** MainController
 * 역할: 홈(개인전/팀전, 2·4인 선택, 규칙보기).
 * 핵심기능: 모드/인원 선택 상태 유지 / 규칙 모달 / 게임 시작→매칭 화면 이동.
 */

package com.omokpang.controller.main;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;

public class MainController {

    @FXML private Label welcomeLabel;
    private String username;

    // 로그인 화면에서 유저 이름을 넘겨받음
    public void setUsername(String username) {
        this.username = username;
        if (welcomeLabel != null) {
            welcomeLabel.setText("환영합니다, " + username + "님!");
        }
    }

    // 로그아웃 버튼 클릭 시 → AuthView.fxml로 돌아감
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auth/AuthView.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("OmokPang - 로그인");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
