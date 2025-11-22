package com.omokpang.controller.splash;

import com.omokpang.domain.user.User;
import com.omokpang.repository.UserRepository;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class RankingController {

    @FXML
    private VBox rankingList;

    private final UserRepository userRepository = new UserRepository();

    @FXML
    public void initialize() {
        loadRanking();
    }

    // 🔙 뒤로가기 버튼 동작
    @FXML
    private void handleBack() {
        com.omokpang.SceneRouter.go("/fxml/main/MainView.fxml");
    }


    private void loadRanking() {
        rankingList.getChildren().clear();

        List<User> users = userRepository.findAllOrderByPoints();

        int rank = 1;
        for (User u : users) {

            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPrefWidth(550);
            row.setPadding(new Insets(15));
            row.setStyle("-fx-background-color: #D0D0D0; -fx-background-radius: 12;");

            // 순위 번호
            Label lblRank = new Label(String.valueOf(rank++));
            lblRank.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

            // 닉네임
            Label lblName = new Label(u.getNickname());
            lblName.setStyle("-fx-font-size: 22px;");

            // 포인트
            Label lblPoint = new Label(u.getPoints() + " ★");
            lblPoint.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

            row.getChildren().addAll(lblRank, lblName, lblPoint);

            rankingList.getChildren().add(row);
        }
    }
}
