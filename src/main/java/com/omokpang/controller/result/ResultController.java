package com.omokpang.controller.result;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.stage.Stage;



public class ResultController {

    @FXML
    private ImageView resultBannerImg;

    @FXML
    private VBox resultBox;

    @FXML
    private VBox rankingBox;

    @FXML
    private ImageView retryImg, exitImg;

    private Image winBanner;
    private Image loseBanner;

    private Image rank1Img, rank2Img, rank3Img, rank4Img;
    private Image starImg;

    @FXML
    public void initialize() {

        // WIN / LOSE
        winBanner  = new Image(getClass().getResource("/images/result/youwin.png").toExternalForm());
        loseBanner = new Image(getClass().getResource("/images/result/youlose.png").toExternalForm());

        // 버튼 이미지
        retryImg.setImage(new Image(getClass().getResource("/images/button/restart_btn.png").toExternalForm()));
        exitImg.setImage(new Image(getClass().getResource("/images/button/exit_btn.png").toExternalForm()));

        // 순위 이미지
        rank1Img = new Image(getClass().getResource("/images/result/rank1.png").toExternalForm());
        rank2Img = new Image(getClass().getResource("/images/result/rank2.png").toExternalForm());
        rank3Img = new Image(getClass().getResource("/images/result/rank3.png").toExternalForm());
        rank4Img = new Image(getClass().getResource("/images/result/rank4.png").toExternalForm());

        // 별 이미지
        starImg = new Image(getClass().getResource("/images/result/star.png").toExternalForm());

        // 테스트 데이터
        boolean isWin = false;
        String[][] ranking = {
                {"1", "비장한 얼룩말", "80", "/images/user/user2.png"},
                {"2", "채채채", "60", "/images/user/user3.png"},
                {"3", "유저2", "40", "/images/user/user2.png"},
                {"4", "user4", "20", "/images/user/user4.png"},
        };

        setResultBanner(isWin);
        loadRanking(ranking);

        Platform.runLater(() -> {
            Stage stage = (Stage) resultBox.getScene().getWindow();
            stage.setWidth(750);
            stage.setHeight(600);
        });

    }


    private void setResultBanner(boolean isWin) {
        resultBannerImg.setImage(isWin ? winBanner : loseBanner);
    }


    // ⭐⭐⭐ Photo UI 완전 동일하게 구현 ⭐⭐⭐
    private void loadRanking(String[][] players) {

        rankingBox.getChildren().clear();

        for (String[] p : players) {

            String rankNum = p[0];
            String name = p[1];
            String score = p[2];
            String avatarPath = p[3];

            // 회색 박스 (사진과 동일)
            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(15));
            row.setPrefWidth(520);
            row.setStyle(
                    "-fx-background-color: #E5E5E5;" +
                            "-fx-background-radius: 25;"
            );

            // ✔ 1) 순위 이미지
            ImageView rankBadge = new ImageView();
            rankBadge.setFitWidth(55);
            rankBadge.setFitHeight(55);

            switch (rankNum) {
                case "1": rankBadge.setImage(rank1Img); break;
                case "2": rankBadge.setImage(rank2Img); break;
                case "3": rankBadge.setImage(rank3Img); break;
                case "4": rankBadge.setImage(rank4Img); break;
            }

            // ✔ 2) 유저 행성 이미지
            ImageView avatar = new ImageView(new Image(getClass().getResource(avatarPath).toExternalForm()));
            avatar.setFitWidth(55);
            avatar.setFitHeight(55);

            // ✔ 3) 닉네임
            Label nameLabel = new Label(name);
            nameLabel.setStyle(
                    "-fx-font-size: 22px;" +
                            "-fx-font-weight: bold;"
            );

            // ✔ 4) 별 이미지
            ImageView star = new ImageView(starImg);
            star.setFitWidth(28);
            star.setFitHeight(28);

            // ✔ 5) 점수 숫자
            Label scoreLabel = new Label(score);
            scoreLabel.setStyle(
                    "-fx-font-size: 20px;" +
                            "-fx-font-weight: bold;"
            );

            // 별 + 점수 묶음
            HBox scoreBox = new HBox(5, star, scoreLabel);
            scoreBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(scoreBox, Priority.ALWAYS);

            // 완성된 row 조합
            row.getChildren().addAll(rankBadge, avatar, nameLabel, scoreBox);

            rankingBox.getChildren().add(row);
        }
    }
}
