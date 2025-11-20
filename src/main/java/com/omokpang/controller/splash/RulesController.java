package com.omokpang.controller.splash;

import com.omokpang.SceneRouter;
import com.omokpang.session.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class RulesController {

    @FXML
    private Button confirmButton;

    // ✅ "확인" 버튼: 로그인 상태면 MainView, 아니면 AuthView 로 이동
    @FXML
    private void handleConfirm() {
        if (AppSession.isLoggedIn()) {
            // 로그인 상태: 메인 화면으로
            SceneRouter.go("/fxml/main/MainView.fxml");
        } else {
            // 비로그인 상태: 로그인 화면으로
            SceneRouter.go("/fxml/auth/AuthView.fxml");
        }
    }
}
