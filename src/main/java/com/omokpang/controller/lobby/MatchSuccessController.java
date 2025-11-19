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

        // 🔥 MatchSession에서 매칭 정보 읽어오기
        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();
        String mode = MatchSession.getMode();
        if (mode == null) mode = "default";

        if (players == null || players.length == 0) {
            // 혹시라도 값이 없으면 기존 하드코딩으로 fallback
            addPlayer("내행성", "/images/user/user3.png");
            addPlayer("상대방", "/images/user/user4.png");
            startCountdownToCardSelect();
            return;
        }

        // ================== 1. 아바타 배정 (같은 방이면 항상 같은 결과) ==================
        // 기본 아바타 후보 4개
        List<String> avatarPool = new ArrayList<>(Arrays.asList(
                "/images/user/user1.png",
                "/images/user/user2.png",
                "/images/user/user3.png",
                "/images/user/user4.png"
        ));

        // 🔑 mode + players 를 이용해 seed 생성 → 같은 매칭이면 두 클라이언트가 동일 seed 사용
        String key = mode + "|" + String.join(",", players);
        long seed = key.hashCode();
        Collections.shuffle(avatarPool, new Random(seed));

        String[] assignedAvatars = new String[players.length];
        for (int i = 0; i < players.length; i++) {
            assignedAvatars[i] = avatarPool.get(i);  // 인원수 <= 4 라고 가정
        }

        // 🔥 이 아바타 정보를 MatchSession에 저장 → GameBoard에서 재사용
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
