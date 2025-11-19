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

        // ⭐⭐ 가장 중요 ⭐⭐
        // GameIntro / GameBoard / MatchSuccess 에서 모두 이 값을 사용하므로
        // 여기에서 꼭 한 번 세팅해둔다.
        MatchSession.setMyNickname(nickname);

        // 3) 1:1 매칭 대기열 등록
        //    형식: QUEUE 1v1 닉네임
        String queueMsg = "QUEUE 1v1 " + nickname;
        System.out.println("[CLIENT] send: " + queueMsg);
        client.send(queueMsg);
    }

    /**
     * 서버에서 오는 모든 문자열을 처리.
     * OmokClient 내부 스레드 → Platform.runLater 로 UI Thread 에서 호출됨.
     */
    private void handleServerMessage(String msg) {
        System.out.println("[UI] MatchingController recv: " + msg);

        // 형식: MATCH 1v1 채채채,채빵
        if (msg.startsWith("MATCH 1v1")) {
            String[] parts = msg.split("\\s+");
            if (parts.length >= 3) {
                String mode = parts[1];          // "1v1"
                String playersPart = parts[2];   // "채채채,채빵"

                String[] players = playersPart.split(",");

                // MatchSession에 저장 (다음 화면들에서 사용)
                MatchSession.setMode(mode);
                MatchSession.setPlayers(players);
            }

            // 매칭 성공 화면으로 이동
            SceneRouter.go("/fxml/lobby/MatchSuccessView.fxml");
        }
    }

    @FXML
    private void onCancel() {
        // 매칭 취소: 그냥 메인으로 돌아가기 (나중에 서버에 CANCEL 보내도 됨)
        SceneRouter.go("/fxml/main/MainView.fxml");
    }
}