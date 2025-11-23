/** SplashController : 시작 화면 컨트롤러.
 * 역할: START 클릭 시 규칙 화면(RulesView)으로 이동.
 * 초기 진입 UI 전용 컨트롤러.
 */

package com.omokpang.controller.splash;

import com.omokpang.SceneRouter;
import javafx.event.ActionEvent;

// 역할/핵심기능: 시작화면 제어 / START 클릭 시 규칙 화면으로 전환
public class SplashController {
    public void handleStart(ActionEvent e) {
        SceneRouter.go("/fxml/splash/RulesView.fxml");
    }
}
