package com.omokpang.controller.game;

import com.omokpang.net.OmokClient;
import com.omokpang.session.MatchSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

/**
 * 역할: 선공/후공 안내 화면.
 *  - MatchSession 정보(players, myNickname)를 기준으로
 *    내가 선공인지 / 후공인지 문구 표시
 *  - 5초 카운트다운 후 GameBoardView로 전환 + 네트워크 바인딩
 *  - 서버에서 오는 TURN / 카드 메시지 → GameBoardController 로 전달
 */
public class GameIntroController {

    @FXML private Label firstPlayerLabel;   // "당신이 선공입니다!" / "당신이 후공입니다!"
    @FXML private Label countdownLabel;     // "5초 뒤에 시작합니다."

    /** 내가 선공인지 여부 */
    private boolean iAmFirst;

    private Timeline countdownTimeline;
    private int remainSeconds = 5;

    /** GameBoardController 인스턴스 (openGameBoard에서 로드 후 저장) */
    private GameBoardController boardController;

    @FXML
    public void initialize() {

        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();

        iAmFirst = false;

        if (players != null && players.length > 0 && me != null) {
            // 약속: players[0] → 선공 유저
            iAmFirst = players[0].equals(me);
        }

        firstPlayerLabel.setText(
                iAmFirst ? "당신이 선공입니다!" : "당신이 후공입니다!"
        );

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


    /* GameBoardView.fxml 로 전환 + NetworkClient 바인딩 + 서버 메시지 처리 등록 */
    private void openGameBoard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/game/GameBoardView.fxml")
            );

            Parent root = loader.load();
            boardController = loader.getController();

            // 1:1 기준 셋업
            boardController.configureForOneVsOne(true);

            // 네트워크 연결 가져오기
            OmokClient client = OmokClient.getInstance();

            // GameBoard → 서버
            boardController.bindNetwork(new GameBoardController.NetworkClient() {

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

                @Override
                public void sendRemoveStart() {
                    client.send("REMOVE_START");
                }

                @Override
                public void sendRemoveTarget(int row, int col) {
                    client.send("REMOVE_TARGET " + row + " " + col);
                }

                @Override
                public void sendShieldBlockForRemove() {
                    client.send("SHIELD_BLOCK_REMOVE");
                }

                @Override
                public void sendShieldBlockForSwap() {
                    client.send("SHIELD_BLOCK_SWAP");
                }

                @Override
                public void sendTurnEnd() {
                    client.send("TURN_END");
                }
            });

            // 서버 → GameBoard 처리
            client.setMessageHandler(line -> {
                Platform.runLater(() -> handleServerMessage(line));
            });

            // 화면 전환
            Stage stage = (Stage) firstPlayerLabel.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 서버 메시지를 GameBoardController 로 전달하는 핵심 처리 */
    private void handleServerMessage(String line) {

        if (boardController == null) return;

        System.out.println("[GameIntro] recv: " + line);

        // 말풍선
        if (line.startsWith("CHEER ")) {
            boardController.onCheerReceivedFromOpponent(
                    line.substring("CHEER ".length())
            );
            return;
        }

        // 상대 돌 두기
        if (line.startsWith("PLACE ")) {
            String[] p = line.split("\\s+");
            if (p.length >= 3) {
                int r = Integer.parseInt(p[1]);
                int c = Integer.parseInt(p[2]);
                boardController.onPlaceFromOpponent(r, c);
            }
            return;
        }

        // 🔄 서버 턴 전달
        if (line.startsWith("TURN ")) {
            boardController.onTurnFromServer(
                    line.substring("TURN ".length()).trim()
            );
            return;
        }

        // 상대방 탈주
        if (line.equals("OPPONENT_LEFT")) {
            boardController.onOpponentLeft();
            return;
        }

        // SharedStone 카드
        if (line.equals("SHARED_STONE_START")) {
            boardController.onSharedStoneStartFromOpponent();
            return;
        }

        if (line.startsWith("SHARED_STONE_TARGET")) {
            String[] p = line.split("\\s+");
            if (p.length >= 3) {
                boardController.onSharedStoneTargetFromOpponent(
                        Integer.parseInt(p[1]),
                        Integer.parseInt(p[2])
                );
            }
            return;
        }

        // Bomb!! 카드
        if (line.equals("BOMB_START")) {
            boardController.onBombStartFromOpponent();
            return;
        }

        if (line.startsWith("BOMB_TARGET")) {
            String[] p = line.split("\\s+");
            if (p.length >= 3)
                boardController.onBombTargetFromOpponent(
                        Integer.parseInt(p[1]),
                        Integer.parseInt(p[2])
                );
            return;
        }

        // Time Lock 카드
        if (line.equals("TIMELOCK_START")) {
            boardController.onTimeLockStartFromOpponent();
            return;
        }

        // Swap 카드
        if (line.equals("SWAP_START")) {
            boardController.onSwapStartFromOpponent();
            return;
        }

        if (line.startsWith("SWAP_TARGET")) {
            String[] p = line.split("\\s+");
            if (p.length >= 5)
                boardController.onSwapTargetFromOpponent(
                        Integer.parseInt(p[1]),
                        Integer.parseInt(p[2]),
                        Integer.parseInt(p[3]),
                        Integer.parseInt(p[4])
                );
            return;
        }

        // Double Move 카드
        if (line.equals("DOUBLE_MOVE_START")) {
            boardController.onDoubleMoveStartFromOpponent();
            return;
        }

        // Remove 카드
        if (line.equals("REMOVE_START")) {
            boardController.onRemoveStartFromOpponent();
            return;
        }

        if (line.startsWith("REMOVE_TARGET")) {
            String[] p = line.split("\\s+");
            if (p.length >= 3)
                boardController.onRemoveTargetFromOpponent(
                        Integer.parseInt(p[1]),
                        Integer.parseInt(p[2])
                );
            return;
        }

        // Shield 방어
        if (line.equals("SHIELD_BLOCK_REMOVE")) {
            boardController.onShieldBlockRemoveFromOpponent();
            return;
        }

        if (line.equals("SHIELD_BLOCK_SWAP")) {
            boardController.onShieldBlockSwapFromOpponent();
            return;
        }
    }
}
