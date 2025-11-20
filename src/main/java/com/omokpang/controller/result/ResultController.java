package com.omokpang.controller.result;

import com.omokpang.SceneRouter;
import com.omokpang.domain.user.User;
import com.omokpang.session.AppSession;
import com.omokpang.service.ResultService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;

/**
 * ResultController
 * 역할: 게임 종료 결과 화면.
 *  - 승/패 배너 보여주기
 *  - 순위/포인트(2인 기준) 표시
 *  - 다시하기(매칭 화면) / 나가기(메인 화면) 전환
 *  - DB(users)에 wins / losses / points 반영 + AppSession 갱신
 */
public class ResultController {

    @FXML
    private ImageView resultBannerImg;

    @FXML
    private VBox resultBox;

    @FXML
    private VBox rankingBox;

    @FXML
    private ImageView retryImg, exitImg;

    @FXML
    private Button retryBtn, exitBtn;

    // 배너 이미지
    private Image winBanner;
    private Image loseBanner;

    // 순위 뱃지 & 별 아이콘
    private Image rank1Img, rank2Img, rank3Img, rank4Img;
    private Image starImg;

    // 서비스
    private final ResultService resultService = ResultService.getInstance();

    /* ---------- 공통 이미지 로더 ---------- */
    private Image loadImage(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            System.out.println("[ResultController] WARN: image not found: " + path);
            return null;
        }
        return new Image(url.toExternalForm());
    }

    @FXML
    public void initialize() {
        // WIN / LOSE 배너
        winBanner  = loadImage("/images/result/youwin.png");
        loseBanner = loadImage("/images/result/youlose.png");

        // 버튼 이미지
        retryImg.setImage(loadImage("/images/button/restart_btn.png"));
        exitImg.setImage(loadImage("/images/button/exit_btn.png"));

        // 순위 뱃지
        rank1Img = loadImage("/images/result/rank1.png");
        rank2Img = loadImage("/images/result/rank2.png");
        rank3Img = loadImage("/images/result/rank3.png");
        rank4Img = loadImage("/images/result/rank4.png");

        // 별 이미지
        starImg = loadImage("/images/result/star.png");

        // (단독 Scene으로 띄우는 경우를 대비해서) 창 크기 고정
        Platform.runLater(() -> {
            if (resultBox.getScene() != null &&
                    resultBox.getScene().getWindow() instanceof Stage stage) {
                stage.setWidth(750);
                stage.setHeight(600);
            }
        });

        // 버튼 동작
        retryBtn.setOnAction(e ->
                SceneRouter.go("/fxml/lobby/MatchingView.fxml")
        );
        exitBtn.setOnAction(e ->
                SceneRouter.go("/fxml/main/MainView.fxml")
        );
    }

    /**
     * GameBoardController 에서 호출:
     * @param isWin   이 클라이언트가 이겼는지 여부
     * @param players [ [순위, 닉네임, 이번 판 포인트(80/40), 아바타경로], ... ]
     */
    public void showResult(boolean isWin, String[][] players) {
        setResultBanner(isWin);
        loadRanking(players);

        // 1) DB(users)에 결과 반영
        resultService.applyGameResult(players);

        // 2) 현재 클라이언트(AppSession)의 유저 정보도 함께 갱신
        applyResultToSession(players);
    }

    /* ---------- 배너 ---------- */
    private void setResultBanner(boolean isWin) {
        Image img = isWin ? winBanner : loseBanner;
        if (img != null) {
            resultBannerImg.setImage(img);
        }
    }

    /* ---------- 순위/점수 리스트 UI ---------- */
    private void loadRanking(String[][] players) {
        rankingBox.getChildren().clear();

        if (players == null) return;

        for (String[] p : players) {

            String rankNum    = p[0];
            String name       = p[1];
            String score      = p[2];
            String avatarPath = p[3];

            HBox row = new HBox(20);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(15));
            row.setPrefWidth(520);
            row.setStyle(
                    "-fx-background-color: #E5E5E5;" +
                            "-fx-background-radius: 25;"
            );

            // 1) 순위 뱃지
            ImageView rankBadge = new ImageView();
            rankBadge.setFitWidth(55);
            rankBadge.setFitHeight(55);
            switch (rankNum) {
                case "1": rankBadge.setImage(rank1Img); break;
                case "2": rankBadge.setImage(rank2Img); break;
                case "3": rankBadge.setImage(rank3Img); break;
                case "4": rankBadge.setImage(rank4Img); break;
                default : rankBadge.setImage(rank4Img);
            }

            // 2) 아바타
            ImageView avatar = new ImageView();
            Image avatarImg = loadImage(avatarPath);
            if (avatarImg != null) avatar.setImage(avatarImg);
            avatar.setFitWidth(55);
            avatar.setFitHeight(55);

            // 3) 닉네임
            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

            // 4) 별 + 점수
            ImageView star = new ImageView();
            if (starImg != null) star.setImage(starImg);
            star.setFitWidth(28);
            star.setFitHeight(28);

            Label scoreLabel = new Label(score);
            scoreLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

            HBox scoreBox = new HBox(5, star, scoreLabel);
            scoreBox.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(scoreBox, Priority.ALWAYS);

            row.getChildren().addAll(rankBadge, avatar, nameLabel, scoreBox);
            rankingBox.getChildren().add(row);
        }
    }

    /* ---------- AppSession(currentUser)에 결과 반영 ---------- */
    private void applyResultToSession(String[][] players) {
        User me = AppSession.getCurrentUser();
        if (me == null || players == null) return;

        String myNick = me.getNickname();

        for (String[] p : players) {
            if (p == null || p.length < 3) continue;

            String nickname  = p[1];
            int rank         = Integer.parseInt(p[0]); // "1" or "2"
            int pointDelta   = Integer.parseInt(p[2]); // 80 or 40
            boolean isWinner = (rank == 1);

            if (!myNick.equals(nickname)) {
                continue;
            }

            // 기존 값
            int newWins    = me.getWins();
            int newLosses  = me.getLosses();
            int newPoints  = me.getPoints();

            // 승/패 반영
            if (isWinner) {
                newWins += 1;
            } else {
                newLosses += 1;
            }

            // 포인트 반영
            newPoints += pointDelta;

            // 🔥 업데이트된 값으로 새 User 인스턴스를 만들어 세션에 다시 저장
            User updated = new User(
                    me.getId(),
                    me.getNickname(),
                    me.getPassword(),
                    newWins,
                    newLosses,
                    newPoints,
                    me.getCreatedAt()
            );

            AppSession.setCurrentUser(updated);
            break;
        }
    }
}