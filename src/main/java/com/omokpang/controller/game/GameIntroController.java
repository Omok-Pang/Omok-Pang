package com.omokpang.controller.game;

import com.omokpang.net.OmokClient;
import com.omokpang.session.MatchSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;

import java.io.IOException;

/**
 * 역할: 선공/후공 안내 화면.
 *  - MatchSession 정보(players, myNickname)를 기준으로
 *    내가 선공인지 / 후공인지 문구 표시
 *  - 5초 카운트다운 후 GameBoardView로 전환 + 네트워크 바인딩
 */
public class GameIntroController {

    @FXML private Label firstPlayerLabel;   // "당신이 선공입니다!" / "당신이 후공입니다!"
    @FXML private Label countdownLabel;     // "5초 뒤에 시작합니다."

    /** 내가 선공인지 여부 */
    private boolean iAmFirst;

    private Timeline countdownTimeline;
    private int remainSeconds = 5;

    @FXML
    public void initialize() {
        // ============================
        //  MatchSession을 기반으로 선/후공 판단
        // ============================
        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();

        iAmFirst = false; // 기본값: 후공
        if (players != null && players.length > 0 && me != null) {
            // 약속: players[0] 이 선공인 플레이어
            iAmFirst = players[0].equals(me);
        }

        // 문구
        firstPlayerLabel.setText(
                iAmFirst ? "당신이 선공입니다!" : "당신이 후공입니다!"
        );

        // 카운트다운 시작
        startCountdown();
    }

    /** 1초마다 감소하는 카운트다운 타이머 */
    private void startCountdown() {
        updateCountdownLabel();

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    remainSeconds--;

                    if (remainSeconds > 0) {
                        updateCountdownLabel();
                    } else {
                        countdownTimeline.stop();
                        // 🔥 여기서 바로 GameBoard 로 전환 + 네트워크 연결
                        openGameBoard();
                    }
                })
        );
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.playFromStart();
    }

    private void updateCountdownLabel() {
        countdownLabel.setText(remainSeconds + "초 뒤에 시작합니다.");
    }

    /**
     * GameBoardView.fxml 을 직접 로드하면서
     * - GameBoardController 가져오기
     * - OmokClient 와 서로 연결
     * - Stage 에 Scene 교체
     */
    private void openGameBoard() {
        try {
            // 1) FXML 로드
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/game/GameBoardView.fxml")
            );
            Parent root = loader.load();

            // 2) 컨트롤러 꺼내오기
            GameBoardController controller = loader.getController();

            // 3) 1:1 모드 레이아웃 설정 (항상 나는 아래)
            controller.configureForOneVsOne(true);

            // 4) 네트워크 클라이언트 가져오기
            OmokClient client = OmokClient.getInstance();

            // 5) GameBoard → 서버 방향 (말풍선, 돌 두기, SharedStone 전송)
            controller.bindNetwork(new GameBoardController.NetworkClient() {
                @Override
                public void sendCheer(String msg) {
                    client.send("CHEER " + msg);
                }

                @Override
                public void sendPlace(int row, int col) {
                    client.send("PLACE " + row + " " + col);
                }

                @Override
                public void sendSharedStoneStart() {
                    client.send("SHARED_STONE_START");
                }

                @Override
                public void sendSharedStoneTarget(int row, int col) {
                    client.send("SHARED_STONE_TARGET " + row + " " + col);
                }

                @Override
                public void sendBombStart() {
                    client.send("BOMB_START");
                }

                @Override
                public void sendBombTarget(int row, int col) {
                    client.send("BOMB_TARGET " + row + " " + col);
                }

                @Override
                public void sendTimeLockStart() {
                    client.send("TIMELOCK_START");
                }

                @Override
                public void sendSwapStart() {
                    client.send("SWAP_START");
                }

                @Override
                public void sendSwapTarget(int myR, int myC, int oppR, int oppC) {
                    client.send("SWAP_TARGET " + myR + " " + myC + " " + oppR + " " + oppC);
                }

                @Override
                public void sendDoubleMoveStart() {
                    client.send("DOUBLE_MOVE_START");
                }
            });

            // 6) 서버 → GameBoard 방향 (메시지 수신 처리)
            client.setMessageHandler(line -> {
                System.out.println("[CLIENT] recv: " + line);

                // 🔥 모든 UI 변경은 JavaFX Application Thread에서 실행
                Platform.runLater(() -> {
                    if (line.startsWith("CHEER ")) {
                        String text = line.substring("CHEER ".length());
                        controller.onCheerReceivedFromOpponent(text);

                    } else if (line.startsWith("PLACE ")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            try {
                                int r = Integer.parseInt(parts[1]);
                                int c = Integer.parseInt(parts[2]);
                                controller.onPlaceFromOpponent(r, c);
                            } catch (NumberFormatException ignored) {}
                        }

                        // 🔥 SharedStone 관련 메시지
                    } else if (line.startsWith("SHARED_STONE_START")) {
                        // 상대가 SharedStone 카드 사용 시작
                        controller.onSharedStoneStartFromOpponent();

                    } else if (line.startsWith("SHARED_STONE_TARGET")) {
                        // 상대가 공용돌로 만든 좌표 수신
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            try {
                                int r = Integer.parseInt(parts[1]);
                                int c = Integer.parseInt(parts[2]);
                                controller.onSharedStoneTargetFromOpponent(r, c);
                            } catch (NumberFormatException ignored) {}
                        }
                    } else if (line.startsWith("BOMB_START")) {
                        // 상대가 Bomb!! 카드 사용 시작
                        controller.onBombStartFromOpponent();
                    } else if (line.startsWith("BOMB_TARGET")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 3) {
                            try {
                                int r = Integer.parseInt(parts[1]);
                                int c = Integer.parseInt(parts[2]);
                                controller.onBombTargetFromOpponent(r, c);
                            } catch (NumberFormatException ignored) {}
                        }
                    } else if (line.startsWith("TIMELOCK_START")) {
                        controller.onTimeLockStartFromOpponent();
                        // 🔥 Swap 관련
                    } else if (line.startsWith("SWAP_START")) {
                        controller.onSwapStartFromOpponent();
                    } else if (line.startsWith("SWAP_TARGET")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 5) {
                            try {
                                int myR  = Integer.parseInt(parts[1]);
                                int myC  = Integer.parseInt(parts[2]);
                                int oppR = Integer.parseInt(parts[3]);
                                int oppC = Integer.parseInt(parts[4]);
                                controller.onSwapTargetFromOpponent(myR, myC, oppR, oppC);
                            } catch (NumberFormatException ignored) {}
                        }
                    } else if (line.startsWith("DOUBLE_MOVE_START")) {
                        // 상대가 DoubleMove 카드를 사용한 경우
                        controller.onDoubleMoveStartFromOpponent();
                    }
                    // MATCH, ECHO 등은 다른 화면에서 처리
                });
            });

            // 7) 실제 화면 전환 (Intro -> Board)
            Stage stage = (Stage) firstPlayerLabel.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}