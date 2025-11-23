/** RulesController : 게임 규칙 화면 컨트롤러.
 * 역할: '확인' 클릭 시 로그인 여부에 따라 MainView 또는 AuthView로 이동.
 * Splash → Rules → Auth/Main 흐름을 제어.
 */

package com.omokpang.controller.splash;

import com.omokpang.SceneRouter;
import com.omokpang.session.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class RulesController {

    @FXML
    private Button confirmButton;

    // "확인" 버튼: 로그인 상태면 MainView, 아니면 AuthView 로 이동
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
