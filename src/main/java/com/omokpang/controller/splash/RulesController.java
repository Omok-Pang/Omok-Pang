/** RulesModalController
 * 역할: 게임 규칙 모달 팝업.
 * 핵심기능: 규칙 텍스트 표시 / 확인 버튼으로 모달 닫기.
 */

package com.omokpang.controller.splash;

import com.omokpang.SceneRouter;
import javafx.event.ActionEvent;

/** 역할/핵심기능: 규칙 화면 제어 / 확인 클릭 시 로그인 화면으로 전환 */
public class RulesController {
    public void handleConfirm(ActionEvent e) {
        SceneRouter.go("/fxml/auth/AuthView.fxml");
    }
}
