package com.omokpang.controller.lobby;

import com.omokpang.SceneRouter;
import com.omokpang.domain.user.User;
import com.omokpang.net.OmokClient;
import com.omokpang.session.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MatchingController {

    @FXML
    private ImageView myAvatar;

    @FXML
    private ImageView cancelButtonImage;

    @FXML
    private Button cancelBtn;

    // 🔥 네트워크 클라이언트 (싱글톤)
    private final OmokClient client = OmokClient.getInstance();

    @FXML
    public void initialize() {

        // ✅ 기존 아바타 이미지 로딩
        Image avatar = new Image(
                getClass().getResource("/images/user/user3.png").toExternalForm()
        );
        myAvatar.setImage(avatar);

        // ============================
        //   취소 버튼 이미지 적용
        // ============================
        Image normal = new Image(
                getClass().getResource("/images/button/match_btn.png").toExternalForm()
        );
        Image hover = new Image(
                getClass().getResource("/images/button/match_btn.png").toExternalForm()
        );

        cancelButtonImage.setImage(normal);
        cancelButtonImage.setOnMouseEntered(e -> cancelButtonImage.setImage(hover));
        cancelButtonImage.setOnMouseExited(e -> cancelButtonImage.setImage(normal));

        // ============================
        //   🔥 서버 매칭 요청 등록
        // ============================

        // 1) 서버에서 오는 메시지를 이 화면이 받도록 핸들러 등록
        client.setMessageHandler(this::handleServerMessage);

        // 2) 내 닉네임 가져오기
        String nickname = "GUEST";
        User user = AppSession.getCurrentUser();
        if (user != null && user.getNickname() != null) {
            nickname = user.getNickname();
        }

        // 3) 1:1 매칭 대기열 등록
        //    형식: QUEUE 1v1 닉네임
        String queueMsg = "QUEUE 1v1 " + nickname;
        System.out.println("[CLIENT] send: " + queueMsg);
        client.send(queueMsg);
    }

    /**
     * 서버에서 오는 모든 문자열을 여기서 처리.
     * OmokClient 내부 스레드 → Platform.runLater 로 UI Thread 에서 호출됨.
     */
    private void handleServerMessage(String msg) {
        System.out.println("[UI] MatchingController recv: " + msg);

        // MATCH 1v1 채채채,채빵
        if (msg.startsWith("MATCH 1v1")) {
            // (지금은 단순히 매칭 성공 화면으로만 이동)
            SceneRouter.go("/fxml/lobby/MatchSuccessView.fxml");
        }
    }

    @FXML
    private void onCancel() {
        // 매칭 취소: 그냥 메인으로 돌아가기 (나중에 서버에 CANCEL 보내도 됨)
        SceneRouter.go("/fxml/main/MainView.fxml");
    }
}
