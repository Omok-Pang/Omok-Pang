package com.omokpang.controller.lobby;

import com.omokpang.SceneRouter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MatchSuccessController {

    @FXML
    private HBox playerBox;

    @FXML
    private ImageView moveButtonImage;

    @FXML
    public void initialize() {

        // 🔥 버튼 이미지 로딩 (MatchingView와 동일)
        Image normal = new Image(
                getClass().getResource("/images/button/match_btn.png").toExternalForm()
        );
        Image hover = new Image(
                getClass().getResource("/images/button/match_btn.png").toExternalForm()
        );
        moveButtonImage.setImage(normal);

        moveButtonImage.setOnMouseEntered(e -> moveButtonImage.setImage(hover));
        moveButtonImage.setOnMouseExited(e -> moveButtonImage.setImage(normal));


        // 🔥 2인 매칭 예시 (원하면 배열 기반 자동 설정도 가능)
        addPlayer("내행성", "/images/user/user3.png");
        addPlayer("상대방", "/images/user/user4.png");
    }

    private void addPlayer(String name, String imgPath) {

        VBox v = new VBox(10);
        v.setAlignment(Pos.CENTER);

        // ⭐ 행성 이미지 (MatchingView와 동일 크기)
        ImageView avatar = new ImageView(
                new Image(getClass().getResource(imgPath).toExternalForm())
        );
        avatar.setFitWidth(200);
        avatar.setFitHeight(200);

        // ⭐ 이름 라벨
        Label label = new Label(name);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 18px;");

        v.getChildren().addAll(avatar, label);
        playerBox.getChildren().add(v);
    }

    @FXML
    private void onMove() {
        SceneRouter.go("/fxml/cards/CardSelectView.fxml");
    }
}