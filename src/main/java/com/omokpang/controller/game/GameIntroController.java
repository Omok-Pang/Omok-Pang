package com.omokpang.controller.game;

import com.omokpang.SceneRouter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.Random;

/**
 * 역할: 선공/후공 안내 화면.
 * 핵심기능: 현재 선/후공 문구 표시 / 5초 카운트다운 후 본게임 화면으로 전환.
 */

public class GameIntroController {

    @FXML private Label firstPlayerLabel;
    @FXML private Label countdownLabel;

    private boolean iAmFirst;
    private Timeline countdownTimeline;
    private int remainSeconds = 5;

    @FXML
    public void initialize() {
        // 임시 선/후공 랜덤
        iAmFirst = new Random().nextBoolean();
        firstPlayerLabel.setText(iAmFirst ? "당신이 후공입니다!" : "당신이 선공입니다!");

        // 카운트다운 시작
        startCountdown();
    }

    private void startCountdown() {
        // 초기 텍스트 설정
        updateCountdownLabel();

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    remainSeconds--;

                    if (remainSeconds > 0) {
                        updateCountdownLabel();
                    } else {
                        // 0초가 되면 타이머 멈추고 게임 화면으로 이동
                        countdownTimeline.stop();
                        SceneRouter.go("/fxml/game/GameBoardView.fxml");
                    }
                })
        );
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.playFromStart();
    }

    private void updateCountdownLabel() {
        countdownLabel.setText(remainSeconds + "초 뒤에 시작합니다.");
    }
}
