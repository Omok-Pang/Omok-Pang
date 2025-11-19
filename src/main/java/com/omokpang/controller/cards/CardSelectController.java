/** CardSelectController
 * 역할: 카드 2장 지급/리롤/선택 완료(20초 제한).
 * 핵심기능: 초기 2장 지급 / 리롤(40pt) / 시간 만료 자동 확정 / 선택 결과 전송.
 */

package com.omokpang.controller.cards;

import com.omokpang.domain.card.Card;
import com.omokpang.service.CardService;
import com.omokpang.service.UserPointService;

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
    private final UserPointService userPointService = new UserPointService();

    private List<Card> cards;

    private Timeline timer;
    private int remain = 20;

    // 현재 로그인 유저 id (이전 화면에서 세팅해줄 값)
    private long userId;

    // 현재 화면에서 사용하는 포인트(= DB의 users.points)
    private int point;

    @FXML
    private void initialize() {
        // point 값은 setUserId()에서 DB 조회 후 세팅
        timerLabel.setStyle("-fx-text-fill: white;");
        startTimer();
    }

    /**
     * 로그인 유저 정보 세팅용
     * - 이전 화면에서 FXMLLoader로 이 컨트롤러를 가져와서 호출해주면 됨.
     *   예) controller.setUserId(loggedInUserId);
     */
    public void setUserId(long userId) {
        this.userId = userId;
        this.point = userPointService.getPoint(userId);
        pointLabel.setText(String.valueOf(point));
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

    /**
     * 포인트 사용 공통 처리
     * - amount 만큼 포인트가 있는지 체크
     * - DB에서 차감(users.points - amount)
     * - 성공하면 로컬 point 도 갱신
     */
    private boolean usePoint(int amount) {
        if (point < amount) {
            // 필요하다면 Alert 로 "포인트 부족" 안내 가능
            return false;
        }

        boolean success = userPointService.decreasePoint(userId, amount);
        if (!success) {
            // 동시접속 등으로 DB 업데이트 실패한 경우 방어
            return false;
        }

        point -= amount;
        pointLabel.setText(String.valueOf(point));
        return true;
    }

    /** 40pt 리롤 1 */
    @FXML
    private void handleReroll1() {
        if (!usePoint(40)) return;

        cards.set(0, cardService.drawOne());
        updateCardImages();
    }

    /** 40pt 리롤 2 */
    @FXML
    private void handleReroll2() {
        if (!usePoint(40)) return;

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game/GameIntroView.fxml"));
            Parent root = loader.load();

            // 컨트롤러 가져오기
            com.omokpang.controller.game.GameBoardController controller = loader.getController();

            // 정석 방식: setter로 카드 전달
            controller.setReceivedCards(cards);

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
