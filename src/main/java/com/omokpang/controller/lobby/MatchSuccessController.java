/** MatchSuccessController : 매칭 성공 후 결과 표시 화면.
 * 역할: 매칭된 플레이어 목록·아바타 표시 / 팀전(2v2) 및 개인전(1v1/4FFA) 자동 세팅.
 * 핵심기능: MatchSession에 팀/아바타 저장 / 5초 카운트다운 후 카드 선택 화면 이동.
 */

package com.omokpang.controller.lobby;

import com.omokpang.SceneRouter;
import com.omokpang.session.MatchSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MatchSuccessController {

    @FXML
    private HBox playerBox;

    @FXML
    private Label countdownLabel;

    private Timeline countdown;
    private int remainSec = 5;

    @FXML
    public void initialize() {

        // MatchSession에서 매칭 정보 읽어오기
        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();
        String mode = MatchSession.getMode();
        if (mode == null) mode = "default";

        if (players == null || players.length == 0) {
            // 값 없을 때 fallback
            addPlayer("내행성", "/images/user/user3.png");
            addPlayer("상대방", "/images/user/user4.png");
            startCountdownToCardSelect();
            return;
        }

        // ================== 1. 팀 정보 / 아바타 배정 ==================
        boolean isTeamMode = "2v2".equals(mode);   // 팀전 여부 확인

        String[] assignedAvatars = new String[players.length];

        if (isTeamMode && players.length == 4) {
            // ---------- 2:2 팀전 ----------

            // 팀 정보: 0팀 / 1팀 (0,1,0,1 고정)
            int[] team = new int[4];
            team[0] = 0; // A팀
            team[1] = 1; // B팀
            team[2] = 0; // A팀
            team[3] = 1; // B팀
            MatchSession.setPlayerTeam(team);

            // 아바타 후보 4개에서 "팀별 대표 아바타 2개"만 사용
            List<String> avatarPool = new ArrayList<>(Arrays.asList(
                    "/images/user/user1.png",
                    "/images/user/user2.png",
                    "/images/user/user3.png",
                    "/images/user/user4.png"
            ));

            // 같은 방이면 두 클라이언트가 항상 같은 조합이 되도록 seed 고정
            String key = mode + "|" + String.join(",", players);
            long seed = key.hashCode();
            Collections.shuffle(avatarPool, new Random(seed));

            String team0Avatar = avatarPool.get(0); // 팀 A 대표 아바타
            String team1Avatar = avatarPool.get(1); // 팀 B 대표 아바타

            for (int i = 0; i < players.length; i++) {
                assignedAvatars[i] = (team[i] == 0) ? team0Avatar : team1Avatar;
            }

        } else {
            // ---------- 개인전 (1v1 / 1v1v1v1) 기존 로직 유지 ----------
            List<String> avatarPool = new ArrayList<>(Arrays.asList(
                    "/images/user/user1.png",
                    "/images/user/user2.png",
                    "/images/user/user3.png",
                    "/images/user/user4.png"
            ));

            String key = mode + "|" + String.join(",", players);
            long seed = key.hashCode();
            Collections.shuffle(avatarPool, new Random(seed));

            for (int i = 0; i < players.length; i++) {
                assignedAvatars[i] = avatarPool.get(i);
            }

            // 개인전이면 playerTeam 은 필요 없으면 null 로 둬도 됨
            if (players.length != 4) {
                MatchSession.setPlayerTeam(null);
            }
        }

        // 이 아바타 정보를 MatchSession에 저장 → GameBoard에서 재사용
        MatchSession.setPlayerAvatars(assignedAvatars);

        // ================== 2. 화면에 플레이어+아바타 표시 ==================
        for (int i = 0; i < players.length; i++) {
            String nick = players[i];
            String labelText = nick.equals(me) ? nick + " (나)" : nick;
            String imgPath = assignedAvatars[i];

            addPlayer(labelText, imgPath);
        }

        // ================== 3. 5초 뒤 카드 선택 화면으로 자동 이동 ==================
        startCountdownToCardSelect();
    }

    private void addPlayer(String name, String imgPath) {

        VBox v = new VBox(10);
        v.setAlignment(Pos.CENTER);

        ImageView avatar = new ImageView(
                new Image(getClass().getResource(imgPath).toExternalForm())
        );
        avatar.setFitWidth(200);
        avatar.setFitHeight(200);

        Label label = new Label(name);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        v.getChildren().addAll(avatar, label);
        playerBox.getChildren().add(v);
    }

    // ===== 5초 카운트다운 후 카드 선택 화면으로 이동 =====
    private void startCountdownToCardSelect() {
        remainSec = 5;
        updateCountdownLabel();

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainSec--;
            if (remainSec <= 0) {
                countdown.stop();
                SceneRouter.go("/fxml/cards/CardSelectView.fxml");
            } else {
                updateCountdownLabel();
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.playFromStart();
    }

    private void updateCountdownLabel() {
        if (countdownLabel != null) {
            countdownLabel.setText(remainSec + "초 뒤에 카드 선택 화면으로 넘어갑니다.");
        }
    }
}
