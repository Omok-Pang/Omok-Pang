package com.omokpang.controller.lobby;

import com.omokpang.SceneRouter;
import com.omokpang.session.MatchSession;
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

        // 🔥 이동 버튼 이미지 로딩
        Image normal = new Image(
                getClass().getResource("/images/button/match_btn.png").toExternalForm()
        );
        Image hover = new Image(
                getClass().getResource("/images/button/match_btn.png").toExternalForm()
        );
        moveButtonImage.setImage(normal);

        moveButtonImage.setOnMouseEntered(e -> moveButtonImage.setImage(hover));
        moveButtonImage.setOnMouseExited(e -> moveButtonImage.setImage(normal));

        // 🔥 MatchSession에서 매칭 정보 읽어오기
        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();

        if (players == null || players.length == 0) {
            // 혹시라도 값이 없으면 기존 하드코딩으로 fallback
            addPlayer("내행성", "/images/user/user3.png");
            addPlayer("상대방", "/images/user/user4.png");
            return;
        }

        // 1:1 기준으로, 0번/1번에 이미지 매핑
        for (int i = 0; i < players.length; i++) {
            String nick = players[i];

            // 내 닉네임이면 "(나)" 표시
            String labelText = nick.equals(me) ? nick + " (나)" : nick;

            String imgPath;
            if (i == 0) {
                imgPath = "/images/user/user1.png";
            } else {
                imgPath = "/images/user/user2.png";
            }

            addPlayer(labelText, imgPath);
        }
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

    @FXML
    private void onMove() {
        SceneRouter.go("/fxml/cards/CardSelectView.fxml");
    }
}
