/** RankingController : 전체 사용자 랭킹 화면 컨트롤러.
 * 역할: DB에서 포인트 순 랭킹 조회 후 UI 리스트로 렌더링.
 * MainView → Ranking 화면 전환 시 동작.
 * UserRepository 사용.
 */

package com.omokpang.controller.splash;

import com.omokpang.domain.user.User;
import com.omokpang.repository.UserRepository;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

    @FXML
    private void handleBack() {
        com.omokpang.SceneRouter.go("/fxml/main/MainView.fxml");
    }

    private void loadRanking() {
        rankingList.getChildren().clear();

        List<User> users = userRepository.findAllOrderByPoints();

        int rank = 1;
        for (User u : users) {

            int currentRank = rank++;

            String baseRowStyle =
                    "-fx-background-color: linear-gradient(to right, #e6e6e6, #f4f4f4);" +
                            "-fx-background-radius: 16;" +
                            "-fx-border-color: rgba(255,255,255,0.3);" +
                            "-fx-border-radius: 16;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0.1, 0, 1);";

            // 1, 2, 3위는 살짝 색 넣기
            if (currentRank == 1) {
                baseRowStyle =
                        "-fx-background-color: linear-gradient(to right, rgba(255,215,0,0.25), rgba(255,245,157,0.55));" +
                                "-fx-background-radius: 16;" +
                                "-fx-border-color: rgba(255,255,255,0.6);" +
                                "-fx-border-radius: 16;" +
                                "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.35), 12, 0.3, 0, 2);";
            } else if (currentRank == 2) {
                baseRowStyle =
                        "-fx-background-color: linear-gradient(to right, rgba(192,192,192,0.25), rgba(230,230,230,0.6));" +
                                "-fx-background-radius: 16;" +
                                "-fx-border-color: rgba(255,255,255,0.6);" +
                                "-fx-border-radius: 16;" +
                                "-fx-effect: dropshadow(gaussian, rgba(192,192,192,0.35), 12, 0.3, 0, 2);";
            } else if (currentRank == 3) {
                baseRowStyle =
                        "-fx-background-color: linear-gradient(to right, rgba(205,127,50,0.25), rgba(239,188,125,0.55));" +
                                "-fx-background-radius: 16;" +
                                "-fx-border-color: rgba(255,255,255,0.6);" +
                                "-fx-border-radius: 16;" +
                                "-fx-effect: dropshadow(gaussian, rgba(205,127,50,0.35), 12, 0.3, 0, 2);";
            }

            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPrefWidth(550);
            row.setPadding(new Insets(15));

            final String rowBaseStyle = baseRowStyle;
            row.setStyle(rowBaseStyle);

            Label lblRank = new Label(String.valueOf(currentRank));
            lblRank.setMinWidth(35);
            lblRank.setStyle(
                    "-fx-font-size: 22px;" +
                            "-fx-font-weight: bold;"
            );

            // 닉네임
            Label lblName = new Label(u.getNickname());
            lblName.setStyle(
                    "-fx-font-size: 22px;"
            );

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label lblPoint = new Label(String.valueOf(u.getPoints()));
            lblPoint.setStyle(
                    "-fx-font-size: 22px;" +
                            "-fx-font-weight: bold;"
            );

            ImageView star = null;
            try {
                Image starImg = new Image(
                        getClass().getResource("/images/result/star.png").toExternalForm()
                );
                star = new ImageView(starImg);
                star.setFitWidth(26);
                star.setFitHeight(26);
                star.setPreserveRatio(true);
            } catch (Exception e) {
                // 이미지 없으면 fallback으로 텍스트 별 사용
                Label starLabel = new Label("★");
                starLabel.setStyle(
                        "-fx-font-size: 22px;" +
                                "-fx-text-fill: #FFD700;" +
                                "-fx-font-weight: bold;"
                );
                row.getChildren().addAll(lblRank, lblName, spacer, starLabel, lblPoint);
                rankingList.getChildren().add(row);
                continue;
            }

            HBox.setMargin(star, new Insets(0, 4, 0, 0));
            row.getChildren().addAll(lblRank, lblName, spacer, star, lblPoint);

            rankingList.getChildren().add(row);
        }
    }
}