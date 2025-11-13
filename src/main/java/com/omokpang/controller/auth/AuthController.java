/** AuthController
 * 역할: 로그인/회원가입 화면 제어.
 * 핵심기능: 입력 유효성(닉≤10, 비번 숫자4) / 로그인·회원가입 요청 / 성공 시 화면 전환.
 */

package com.omokpang.controller.auth;

import com.omokpang.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class AuthController {

    // ----- FXML 바인딩 (fx:id와 1:1 일치) -----
    @FXML private TabPane authTabs;

    // 로그인 탭
    @FXML private TextField loginIdField;
    @FXML private PasswordField loginPwField;
    @FXML private Label loginMsgLabel;

    // 회원가입 탭
    @FXML private TextField signupIdField;
    @FXML private PasswordField signupPwField;
    @FXML private PasswordField signupPwConfirmField;
    @FXML private Label signupMsgLabel;
    @FXML private Button signupButton; // 버튼 활성/비활성 제어 (FXML에서 fx:id 추가 필요)

    private final AuthService authService = new AuthService();

    // 초기화: 회원가입 버튼 비활성 + 실시간 유효성 검사
    @FXML
    private void initialize() {
        if (signupButton != null) signupButton.setDisable(true);

        // 실시간 유효성 검사 → 조건 만족 시 버튼 활성화
        if (signupIdField != null && signupPwField != null && signupPwConfirmField != null) {
            signupIdField.textProperty().addListener((obs, o, n) -> validateSignupInputs());
            signupPwField.textProperty().addListener((obs, o, n) -> validateSignupInputs());
            signupPwConfirmField.textProperty().addListener((obs, o, n) -> validateSignupInputs());
        }
    }

    // ---------- 로그인 ----------
    @FXML
    private void handleLogin() {
        String id = safeText(loginIdField);
        String pw = safeText(loginPwField);

        if (id.isEmpty() || pw.isEmpty()) {
            showLoginError("닉네임과 비밀번호를 모두 입력하세요.");
            return;
        }

        boolean ok = authService.login(id, pw);
        if (ok) {
            loginMsgLabel.setText("");
            goToMainView();
        } else {
            showLoginError("잘못된 닉네임 또는 비밀번호입니다.");
        }
    }

    // ---------- 회원가입 ----------
    @FXML
    private void handleSignup() {
        String id = safeText(signupIdField);
        String pw = safeText(signupPwField);
        String confirm = safeText(signupPwConfirmField);

        // 조건: 닉네임(<=10), 비번(숫자4), 확인 일치
        if (id.isEmpty() || pw.isEmpty() || confirm.isEmpty()) {
            showSignupError("모든 필드를 입력하세요.");
            return;
        }
        if (id.length() > 10) {
            showSignupError("닉네임은 10자 이내여야 합니다.");
            return;
        }
        if (!pw.matches("\\d{4}")) {
            showSignupError("비밀번호는 숫자 4자리로 입력하세요.");
            return;
        }
        if (!pw.equals(confirm)) {
            showSignupError("비밀번호가 일치하지 않습니다.");
            return;
        }

        boolean created = authService.signup(id, pw);
        if (created) {
            signupMsgLabel.setStyle("-fx-text-fill: #3bd16f;");
            signupMsgLabel.setText("회원가입 성공! 로그인 탭으로 이동합니다.");
            // 로그인 탭으로 이동 + 편의상 입력 채워줌
            authTabs.getSelectionModel().select(0);
            loginIdField.setText(id);
            loginPwField.setText(pw);
        } else {
            showSignupError("이미 존재하는 닉네임입니다.");
        }
    }

    // ---------- 탭 전환 ----------
    @FXML
    private void handleGotoSignup() {
        authTabs.getSelectionModel().select(1);
        if (signupMsgLabel != null) signupMsgLabel.setText("");
    }

    @FXML
    private void handleGotoLogin() {
        authTabs.getSelectionModel().select(0);
        if (loginMsgLabel != null) loginMsgLabel.setText("");
    }

    // ---------- 유틸 ----------
    private void validateSignupInputs() {
        if (signupButton == null) return;

        String id = safeText(signupIdField);
        String pw = safeText(signupPwField);
        String confirm = safeText(signupPwConfirmField);

        boolean idOk = !id.isEmpty() && id.length() <= 10;
        boolean pwOk = pw.matches("\\d{4}");
        boolean confirmOk = pw.equals(confirm) && !confirm.isEmpty();

        signupButton.setDisable(!(idOk && pwOk && confirmOk));
        // 안내 메시지는 눌렀을 때만 띄우도록 유지 (과도한 경고 방지)
    }

    private void showLoginError(String msg) {
        loginMsgLabel.setStyle("-fx-text-fill: red;");
        loginMsgLabel.setText(msg);
    }

    private void showSignupError(String msg) {
        signupMsgLabel.setStyle("-fx-text-fill: red;");
        signupMsgLabel.setText(msg);
    }

    private String safeText(TextInputControl c) {
        return c == null || c.getText() == null ? "" : c.getText().trim();
    }

    private void goToMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main/MainView.fxml"));
            Parent mainRoot = loader.load();

            // ✅ 로그인한 닉네임 전달
            com.omokpang.controller.main.MainController controller = loader.getController();
            //controller.setUsername(loginIdField.getText());

            Stage stage = (Stage) loginIdField.getScene().getWindow();
            stage.setScene(new Scene(mainRoot));
            stage.setTitle("OmokPang - 메인 화면");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            loginMsgLabel.setText("화면 전환 오류 발생");
        }
    }

}
