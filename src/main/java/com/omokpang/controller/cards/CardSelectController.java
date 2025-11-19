/** CardSelectController
 * 역할: 카드 2장 지급/리롤/선택 완료(20초 제한).
 * 핵심기능: 초기 2장 지급 / 리롤(40pt) / 시간 만료 자동 확정 / 선택 결과 전송.
 */

package com.omokpang.controller.cards;

import com.omokpang.domain.card.Card;
import com.omokpang.service.CardService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

import javafx.scene.layout.VBox;

public class CardSelectController {

    @FXML private VBox beforeBox;
    @FXML private VBox afterBox;

    @FXML private ImageView cardImage1;
    @FXML private ImageView cardImage2;

    @FXML private Button rerollBtn1;
    @FXML private Button rerollBtn2;
    @FXML private Button receiveBtn;
    @FXML private Button completeBtn;

    @FXML private Label timerLabel;
    @FXML private Label pointLabel;

    private final CardService cardService = new CardService();
    private List<Card> cards;

    private Timeline timer;
    private int remain = 20;

    private int point = 120;  // 테스트용

    @FXML
    private void initialize() {
        pointLabel.setText(String.valueOf(point));

        timerLabel.setStyle("-fx-text-fill: white;");
        startTimer();
    }

    /** 카드 받기 */
    @FXML
    private void handleReceive() {
        cards = cardService.drawTwo();  // 카드 두 장 뽑기
        updateCardImages();             // 카드 이미지 적용

        // UI 전환
        beforeBox.setVisible(false);
        afterBox.setVisible(true);


        // 카드 받기 버튼은 다시 못 누르게
        receiveBtn.setDisable(true);
    }

    /** 카드 이미지 반영 */
    private void updateCardImages() {
        setImage(cardImage1, cards.get(0).getImagePath());
        setImage(cardImage2, cards.get(1).getImagePath());
    }

    private void setImage(ImageView iv, String path) {
        iv.setImage(new Image(getClass().getResource(path).toExternalForm()));
    }

    /** 40pt 리롤 1 */
    @FXML
    private void handleReroll1() {
        if (point < 40) return;

        point -= 40;
        pointLabel.setText(String.valueOf(point));

        cards.set(0, cardService.drawOne());
        updateCardImages();
    }

    /** 40pt 리롤 2 */
    @FXML
    private void handleReroll2() {
        if (point < 40) return;

        point -= 40;
        pointLabel.setText(String.valueOf(point));

        cards.set(1, cardService.drawOne());
        updateCardImages();
    }

    /** 20초 카운트다운 */
    private void startTimer() {
        remain = 20;
        timerLabel.setText("20초");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remain--;
            timerLabel.setText(remain + "초");

            if (remain <= 0) {
                finishSelection();
            }
        }));

        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    /** 선택 완료 */
    @FXML
    private void handleComplete() {
        finishSelection();
    }

    /** 최종 두 장 GameBoardController로 전달 */
    private void finishSelection() {
        if (timer != null) timer.stop();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game/GameBoardView.fxml"));
            Parent root = loader.load();

            // 컨트롤러 가져오기
            com.omokpang.controller.game.GameBoardController controller = loader.getController();

            // 정석 방식: setter로 카드 전달
            //controller.setReceivedCards(cards);

            Stage stage = (Stage) cardImage1.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleOpenCatalog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cards/CardCatalogModal.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("카드 종류");
            stage.initOwner(receiveBtn.getScene().getWindow());
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}