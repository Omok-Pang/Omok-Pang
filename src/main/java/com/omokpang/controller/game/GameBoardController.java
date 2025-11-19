package com.omokpang.controller.game;

import com.omokpang.controller.effect.TimeLockNoticeController;
import com.omokpang.controller.effect.SwapSelectGuideController;
import com.omokpang.controller.effect.SwapNoticeController;
import com.omokpang.domain.card.Card;
import com.omokpang.session.MatchSession;   // 🔥 MatchSession 사용
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class GameBoardController {

    // ====== 외부에서 연결할 인터페이스(응원 메시지 전송용) ======
    @FunctionalInterface
    public interface CheerSender {
        void sendCheer(String message);
    }

    private CheerSender cheerSender;  // WebSocket 등으로 실제 전송하는 쪽

    public void setCheerSender(CheerSender cheerSender) {
        this.cheerSender = cheerSender;
    }

    // 1:1 여부 / 내가 아래인지 여부
    private boolean oneVsOne = true;
    private boolean meIsBottom = true;   // true: 나는 아래, false: 나는 위

    // 🔥 프로필별 돌 이미지 경로
    private String topStonePath = "/images/user/sm_user1.png";
    private String bottomStonePath = "/images/user/sm_user2.png";

    // 루트 레이아웃
    @FXML private BorderPane rootPane;

    // center 영역 최상단 StackPane (오버레이를 얹을 컨테이너)
    @FXML private StackPane centerStack;

    // 보드 UI (360x360 Pane)
    @FXML private Pane boardRoot;

    // 상단 타이머 / 턴 안내
    @FXML private Label timerLabel;
    @FXML private Label turnLabel;

    // 위/아래 플레이어 아바타 컨테이너 및 이미지
    @FXML private StackPane topPlayerContainer;
    @FXML private StackPane bottomPlayerContainer;
    @FXML private ImageView topPlayerImage;
    @FXML private ImageView bottomPlayerImage;

    // 좌/우 플레이어 아바타 컨테이너 및 이미지 (4인용 자리)
    @FXML private StackPane leftPlayerContainer;
    @FXML private StackPane rightPlayerContainer;
    @FXML private ImageView leftPlayerImage;
    @FXML private ImageView rightPlayerImage;

    // 말풍선 버튼 (왼쪽 아래)
    @FXML private Button messageButton;

    // 왼쪽 말풍선 선택 패널
    @FXML private StackPane messageSelectPane;   // 전체 패널
    @FXML private VBox messageListBox;          // 패널 안의 메시지 목록 컨테이너

    // 위/아래 유저 말풍선 영역 (말풍선 이미지 + 텍스트)
    @FXML private StackPane topMessageBubble;
    @FXML private Label topMessageLabel;
    @FXML private StackPane bottomMessageBubble;
    @FXML private Label bottomMessageLabel;

    // 선택된 카드 아이콘 표시 영역 (오른쪽 아래)
    @FXML private HBox cardSlotBox;

    // 카드 선택 화면에서 전달받은 카드 두 장
    private List<Card> receivedCards;

    // ================== 보드 / 턴 관련 상수 & 상태 ==================
    private static final int N = 15;            // 보드 크기 (15 x 15)
    private static final double SIZE = 360;     // 보드 한 변 길이(px)
    private static final double CELL = SIZE / (N - 1); // 한 칸(격자 간격) 크기

    // 보드 상태: 0=빈칸, 1=위 유저의 돌, -1=아래 유저의 돌
    private final int[][] board = new int[N][N];

    // 현재 턴(누가 둘 차례인지): 1=위 유저, -1=아래 유저
    private int current = 1;

    // ================== 타이머 관련 ==================
    private static final int DEFAULT_TURN_SECONDS = 20; // 기본 턴 시간
    private static final int TIMELOCK_TURN_SECONDS = 3; // Time Lock 적용 시 턴 시간

    private Timeline timer;   // 1초마다 동작하는 타이머
    private int remain = DEFAULT_TURN_SECONDS;  // 남은 시간(초)

    // ================== 프리셋 말풍선 텍스트 ==================
    private static final String[] PRESET_MESSAGES = {
            "빵야빵야 오목팡!",
            "얼른 놔라팡",
            "즐겁팡",
            "한팡 더?",
            "나랑 놀아줘팡",
            "넌 이미 졌팡...",
            "돌아버리겠팡",
            "이거 실화팡?",
            "오목팡 최고팡",
            "위기탈출팡",
            "반전팡!",
            "쫄깃쫄깃팡",
            "거기 두지 마팡",
            "망했팡...",
            "다음 판엔 이긴다팡"
    };

    // ================== Swap 카드 관련 상태 ==================
    private SwapSelectGuideController swapGuideController;
    private boolean swapSelecting = false;

    // ================== 외부에서 플레이어 배치 설정 ==================
    public void configureForOneVsOne(boolean meIsBottom) {
        this.oneVsOne = true;
        this.meIsBottom = meIsBottom;
        applyLayoutConfig();
        updateTurnLabel();
        updateActivePlayerHighlight();
    }

    private void applyLayoutConfig() {
        // 1:1 이면 좌우 프로필 숨기기
        boolean sideVisible = !oneVsOne;

        if (leftPlayerContainer != null) {
            leftPlayerContainer.setVisible(sideVisible);
            leftPlayerContainer.setManaged(sideVisible);
        }
        if (rightPlayerContainer != null) {
            rightPlayerContainer.setVisible(sideVisible);
            rightPlayerContainer.setManaged(sideVisible);
        }
    }

    // ================== 초기화 ==================
    @FXML
    public void initialize() {
        // 아바타 컨테이너가 가로로 쭉 늘어지지 않도록
        bottomPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
        bottomPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);
        topPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
        topPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);

        if (leftPlayerContainer != null) {
            leftPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
            leftPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);
        }
        if (rightPlayerContainer != null) {
            rightPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
            rightPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);
        }

        // 말풍선 기본은 숨김
        messageSelectPane.setVisible(false);
        topMessageBubble.setVisible(false);
        bottomMessageBubble.setVisible(false);

        // 기본은 1:1 + 나는 아래라고 가정
        applyLayoutConfig();

        // 🔥 MatchSession에서 아바타 정보 읽어서 프로필/돌 세팅
        initAvatarsFromSession();

        // 🔥 여기서 내가 선택한 카드 두 장 세팅
        List<Card> myCards = MatchSession.getMySelectedCards();
        if (myCards != null && !myCards.isEmpty()) {
            setReceivedCards(myCards);
        }

        // 보드 그리기
        boardRoot.setPrefSize(SIZE, SIZE);
        drawGrid();

        // 보드 클릭
        boardRoot.setOnMouseClicked(e -> {
            int c = (int) Math.round(e.getX() / CELL);
            int r = (int) Math.round(e.getY() / CELL);
            place(r, c);
        });

        // 말풍선 리스트, 턴 정보, 타이머 시작
        setupMessageList();
        updateTurnLabel();
        updateActivePlayerHighlight();
        startTurn();
    }

    /**
     * MatchSuccess에서 저장해둔 아바타 정보를 이용해
     * - top / bottom 프로필 이미지
     * - topStonePath / bottomStonePath
     * 를 세팅한다.
     */
    private void initAvatarsFromSession() {
        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();
        String[] avatars = MatchSession.getPlayerAvatars();

        if (players == null || avatars == null || players.length < 2) {
            // 세션 정보가 없으면 FXML 기본 이미지 + 기본 돌 사용
            return;
        }

        // 현재는 1:1 기준: players[0] → 위, players[1] → 아래
        String topAvatar = avatars[0];
        String bottomAvatar = avatars[1];

        // 프로필 이미지 적용
        topPlayerImage.setImage(
                new Image(getClass().getResource(topAvatar).toExternalForm())
        );
        bottomPlayerImage.setImage(
                new Image(getClass().getResource(bottomAvatar).toExternalForm())
        );

        // 프로필에 맞는 돌 이미지 경로 세팅
        topStonePath = toStonePath(topAvatar);       // user1.png → sm_user1.png
        bottomStonePath = toStonePath(bottomAvatar); // user2.png → sm_user2.png

        // 내 위치(위/아래) 계산: players 배열에서 내 닉네임 위치 찾기
        int myIndex = 0;
        for (int i = 0; i < players.length; i++) {
            if (players[i].equals(me)) {
                myIndex = i;
                break;
            }
        }
        // 1:1 기준으로 index 0=위, 1=아래
        meIsBottom = (myIndex == 1);
    }

    /**
     * "/images/user/user1.png" → "/images/user/sm_user1.png" 으로 바꿔주는 헬퍼.
     */
    private String toStonePath(String avatarPath) {
        if (avatarPath == null) return "/images/user/sm_user1.png";
        // 파일명이 user1.png, user2.png ... 라고 가정
        // "/images/user/user1.png".replace("user", "sm_user") → "/images/user/sm_user1.png"
        return avatarPath.replace("/user", "/sm_user");
    }

    // ================== 말풍선 리스트 UI 구성 ==================
    private void setupMessageList() {
        messageListBox.getChildren().clear();
        for (String text : PRESET_MESSAGES) {
            Region item = createMessageItem(text);
            messageListBox.getChildren().add(item);
        }
    }

    private Region createMessageItem(String text) {
        Image bgImg = new Image(
                getClass().getResource("/images/message/ui_select.png").toExternalForm()
        );
        ImageView bgView = new ImageView(bgImg);
        bgView.setPreserveRatio(true);
        bgView.setFitWidth(200);

        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: #000000;" +
                        "-fx-font-size: 14;" +
                        "-fx-font-weight: 700;"
        );

        StackPane item = new StackPane(bgView, label);
        item.setPrefWidth(200);
        item.setMaxWidth(200);

        item.setOnMouseClicked(e -> sendBalloon(text));

        VBox.setMargin(item, new Insets(2, 0, 2, 0));
        return item;
    }

    // ================== 보드 그리기 및 돌 놓기 ==================
    private void drawGrid() {
        boardRoot.getChildren().clear();

        for (int i = 0; i < N; i++) {
            double p = i * CELL;
            Line h = new Line(0, p, SIZE, p);
            Line v = new Line(p, 0, p, SIZE);

            h.setStroke(Color.color(1, 1, 1, 0.25));
            v.setStroke(Color.color(1, 1, 1, 0.25));
            boardRoot.getChildren().addAll(h, v);
        }

        Line b1 = new Line(0, 0, SIZE, 0);
        Line b2 = new Line(SIZE, 0, SIZE, SIZE);
        Line b3 = new Line(SIZE, SIZE, 0, SIZE);
        Line b4 = new Line(0, SIZE, 0, 0);
        for (Line b : new Line[]{b1, b2, b3, b4}) {
            b.setStroke(Color.color(1, 1, 1, 0.6));
            b.setStrokeWidth(2);
        }
        boardRoot.getChildren().addAll(b1, b2, b3, b4);
    }

    private void place(int r, int c) {
        if (!isInside(r, c) || board[r][c] != 0) return;

        double cx = c * CELL;
        double cy = r * CELL;

        // 🔥 유저별 돌 이미지 경로 (프로필에 매칭된 돌 사용)
        String stonePath = (current == 1)   // 1 = 위 플레이어
                ? topStonePath
                : bottomStonePath;

        Image img = new Image(getClass().getResource(stonePath).toExternalForm());
        ImageView stone = new ImageView(img);

        double stoneSize = CELL * 0.9;
        stone.setFitWidth(stoneSize);
        stone.setFitHeight(stoneSize);
        stone.setPreserveRatio(true);

        stone.setLayoutX(cx - stoneSize / 2);
        stone.setLayoutY(cy - stoneSize / 2);

        boardRoot.getChildren().add(stone);
        board[r][c] = current;

        // TODO: 승리 조건 검사 / 서버 전송

        current *= -1;
        updateTurnLabel();
        updateActivePlayerHighlight();
        restartTimer();
    }

    private boolean isInside(int r, int c) {
        return r >= 0 && r < N && c >= 0 && c < N;
    }

    // ================== 아바타 하이라이트 / 턴 텍스트 ==================
    private void updateActivePlayerHighlight() {
        String activeStyle =
                "-fx-padding: 6;" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: #ff4d4f;" +
                        "-fx-border-width: 4;" +
                        "-fx-border-radius: 999;";

        String inactiveStyle =
                "-fx-padding: 6;" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-width: 4;" +
                        "-fx-border-radius: 999;";

        // current == 1 : 위 플레이어 턴
        boolean topTurn = (current == 1);

        if (topTurn) {
            topPlayerContainer.setStyle(activeStyle);
            bottomPlayerContainer.setStyle(inactiveStyle);
        } else {
            topPlayerContainer.setStyle(inactiveStyle);
            bottomPlayerContainer.setStyle(activeStyle);
        }
    }

    private void updateTurnLabel() {
        // current == 1 : 위 플레이어 턴
        boolean topTurn = (current == 1);

        if (meIsBottom) {
            // 나는 아래
            if (topTurn) {
                turnLabel.setText("상대 턴 (위 유저)");
            } else {
                turnLabel.setText("내 턴 (아래 유저)");
            }
        } else {
            // 나는 위
            if (topTurn) {
                turnLabel.setText("내 턴 (위 유저)");
            } else {
                turnLabel.setText("상대 턴 (아래 유저)");
            }
        }
    }

    // ================== 턴 타이머 로직 ==================
    private void startTurn() {
        startTurnWithSeconds(DEFAULT_TURN_SECONDS);
    }

    private void startTurnWithSeconds(int seconds) {
        stopTimer();

        remain = seconds;
        timerLabel.setText(remain + "초");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remain--;
            timerLabel.setText(remain + "초");

            if (remain <= 0) {
                // 시간 초과 → 턴 넘기기
                current *= -1;
                updateTurnLabel();
                updateActivePlayerHighlight();
                restartTimer();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.playFromStart();
    }

    private void restartTimer() {
        stopTimer();
        startTurn();
    }

    private void stopTimer() {
        if (timer != null) timer.stop();
    }

    // ================== 말풍선 플로우 ==================
    @FXML
    public void handleCheer() {
        boolean nowVisible = messageSelectPane.isVisible();
        messageSelectPane.setVisible(!nowVisible);
    }

    private void sendBalloon(String text) {
        messageSelectPane.setVisible(false);
        showMyBalloon(text);

        if (cheerSender != null) {
            cheerSender.sendCheer(text);
        }
    }

    private void showMyBalloon(String text) {
        if (meIsBottom) {
            showBalloonOn(bottomMessageBubble, bottomMessageLabel, text);
        } else {
            showBalloonOn(topMessageBubble, topMessageLabel, text);
        }
    }

    public void onCheerReceivedFromOpponent(String text) {
        if (meIsBottom) {
            showBalloonOn(topMessageBubble, topMessageLabel, text);
        } else {
            showBalloonOn(bottomMessageBubble, bottomMessageLabel, text);
        }
    }

    private void showBalloonOn(StackPane bubble, Label label, String text) {
        label.setText(text);
        bubble.setVisible(true);

        PauseTransition hide = new PauseTransition(Duration.seconds(2));
        hide.setOnFinished(e -> bubble.setVisible(false));
        hide.play();
    }

    // ================== 카드 선택 모달 / TimeLock / Swap / 카드 슬롯 (기존 그대로) ==================
    // ... (여기부터는 네가 줬던 코드 그대로 두면 돼, 위에서 바꾼 부분은 돌/아바타 관련만이야) ...
    /**
     * 카드 선택 화면에서 받은 카드 2장을 GameBoard에 표시하는 메서드.
     * - MatchSession에서 가져온 카드들을 UI 슬롯에 채운다.
     */
    public void setReceivedCards(List<Card> cards) {
        this.receivedCards = cards;

        if (cards == null || cards.isEmpty()) return;

        // cardSlotBox 초기화
        cardSlotBox.getChildren().clear();

        for (Card card : cards) {
            ImageView iv = new ImageView(
                    new Image(getClass().getResource(card.getImagePath()).toExternalForm())
            );

            iv.setFitWidth(40);
            iv.setFitHeight(40);
            iv.setPreserveRatio(true);

            // 카드마다 테두리 스타일
            iv.setStyle("-fx-effect: dropshadow(gaussian, black, 4, 0, 0, 0);");

            cardSlotBox.getChildren().add(iv);
        }
    }
}
