/** SplashController
 * 역할: 시작 화면 제어(START, 규칙 보기).
 * 핵심기능: START 클릭 시 로그인 화면 이동 / 규칙 모달 열기 / 초기 UI 세팅.
 */

package com.omokpang.controller.splash;

import com.omokpang.SceneRouter;
import javafx.event.ActionEvent;

/** 역할/핵심기능: 시작화면 제어 / START 클릭 시 규칙 화면으로 전환 */
public class SplashController {
    public void handleStart(ActionEvent e) {
        SceneRouter.go("/fxml/splash/RulesView.fxml");
    }
}
