package com.omokpang.controller.lobby;

import com.omokpang.SceneRouter;
import com.omokpang.domain.user.User;
import com.omokpang.net.OmokClient;
import com.omokpang.session.AppSession;
import com.omokpang.session.MatchSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * 역할: 매칭 대기 화면.
 *  - 서버에 "QUEUE 1v1 닉네임" 전송
 *  - 서버에서 "MATCH 1v1 A,B" 수신 시 MatchSession에 저장 후
 *    MatchSuccess 화면으로 이동.
 */
public class MatchingController {

    @FXML
    private ImageView myAvatar;

    @FXML
    private ImageView cancelButtonImage;

    @FXML
    private Button cancelBtn;

    /** 네트워크 클라이언트 (싱글톤) */
    private final OmokClient client = OmokClient.getInstance();

    @FXML
    public void initialize() {

        // ============================
        //   내 아바타 기본 이미지 로딩
        //   (나중에 실제 유저 아바타로 바꿔도 됨)
        // ============================
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
        //   서버 매칭 요청 등록
        // ============================

        // 1) 서버에서 오는 메시지를 이 화면이 받도록 핸들러 등록
        client.setMessageHandler(this::handleServerMessage);

        // 2) 내 닉네임 가져오기 (AppSession → 없으면 "GUEST")
        String nickname = "GUEST";
        User user = AppSession.getCurrentUser();
        if (user != null && user.getNickname() != null) {
            nickname = user.getNickname();
        }

        // 🔥 여기서 MatchSession에 내 닉네임 저장!
        MatchSession.setMyNickname(nickname);

        // 3) 내가 원하는 모드 가져오기 (없으면 기본 1v1)
        String modeToQueue = MatchSession.getRequestedMode();
        if (modeToQueue == null || modeToQueue.isBlank()) {
            modeToQueue = "1v1";
        }

        // "QUEUE <mode> <nickname>"
        String queueMsg = "QUEUE " + modeToQueue + " " + nickname;
        System.out.println("[CLIENT] send: " + queueMsg);
        client.send(queueMsg);
    }

    /**
     * 서버에서 오는 모든 문자열을 처리.
     * OmokClient 내부 스레드 → Platform.runLater 로 UI Thread 에서 호출됨.
     */
    private void handleServerMessage(String msg) {
        System.out.println("[UI] MatchingController recv: " + msg);

        if (msg.startsWith("MATCH ")) {
            // 예) MATCH 1v1 채채채,채빵
            //    MATCH 1v1v1v1 A,B,C,D
            String[] parts = msg.split("\\s+");
            if (parts.length >= 3) {
                String mode = parts[1];        // "1v1" 또는 "1v1v1v1"
                String playersPart = parts[2]; // "A,B" 또는 "A,B,C,D"

                String[] players = playersPart.split(",");

                MatchSession.setMode(mode);
                MatchSession.setPlayers(players);
            }

            // 모드는 일단 상관없이 같은 매칭 성공 화면 재사용해도 됨
            SceneRouter.go("/fxml/lobby/MatchSuccessView.fxml");
        }
    }

    @FXML
    private void onCancel() {
        // 매칭 취소: 그냥 메인으로 돌아가기 (나중에 서버에 CANCEL 보내도 됨)
        SceneRouter.go("/fxml/main/MainView.fxml");
    }
}