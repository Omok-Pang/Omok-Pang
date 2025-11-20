package com.omokpang.controller.game;

import com.omokpang.controller.effect.TimeLockNoticeController;
import com.omokpang.controller.effect.SwapSelectGuideController;
import com.omokpang.controller.effect.SwapNoticeController;
import com.omokpang.domain.card.Card;
import com.omokpang.session.MatchSession;   // 🔥 MatchSession 사용

import com.omokpang.controller.result.ResultController;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
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

/**
 * 역할: 실제 오목판 화면.
 *  - 위/아래(좌/우) 플레이어 아바타 표시
 *  - MatchSession에서 아바타/닉네임/카드 정보를 가져와 배치
 *  - 돌 두기 / 턴 전환 / 타이머 / 말풍선 / 카드 효과 처리
 */
public class GameBoardController {

    // ====== 외부에서 연결할 인터페이스(응원 메시지 전송용) ======
    @FunctionalInterface
    public interface CheerSender {
        void sendCheer(String message);
    }

    /** 말풍선 텍스트를 서버로 보내는 실제 구현체 (NetworkClient 래핑) */
    private CheerSender cheerSender;

    public void setCheerSender(CheerSender cheerSender) {
        this.cheerSender = cheerSender;
    }

    // 1:1 여부 / 내가 아래인지 여부
    private boolean oneVsOne = true;
    private boolean meIsBottom = true;   // true: 나는 아래, false: 나는 위 (현재는 항상 true)

    // 프로필별 기본 돌 이미지 경로 (fallback 용)
    private String topStonePath = "/images/user/sm_user1.png";
    private String bottomStonePath = "/images/user/sm_user2.png";

    // ---- 내/상대 턴 정보 (1 = 선공, -1 = 후공) ----
    /** 나는 선공(1)인지 후공(-1)인지 */
    private int mySign = 1;
    /** 상대는 항상 나의 반대 */
    private int opponentSign = -1;

    // 내 돌 / 상대 돌 이미지 경로 (sm_ 아이콘)
    private String myStonePath;
    private String opponentStonePath;

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
    /**
     * 1:1 모드 레이아웃 설정.
     * 현재는 항상 "나는 아래" 로 고정.
     */
    public void configureForOneVsOne(boolean ignore) {
        this.oneVsOne = true;
        this.meIsBottom = true;

        applyLayoutConfig();
        updateTurnLabel();
        updateActivePlayerHighlight();
    }

    /** 1:1일 때 좌/우 아바타 숨기기 */
    private void applyLayoutConfig() {
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
        // 아바타 컨테이너가 가로로 쭉 늘어지지 않도록 프리사이즈 유지
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

        // 🔥 MatchSession에서 아바타/닉네임 정보 읽어서 프로필 & 돌 세팅
        initAvatarsFromSession();

        // 🔥 선택한 카드 두 장 세팅 (있을 경우)
        List<Card> myCards = MatchSession.getMySelectedCards();
        if (myCards != null && !myCards.isEmpty()) {
            setReceivedCards(myCards);
        }

        // 보드 그리기 (격자)
        boardRoot.setPrefSize(SIZE, SIZE);
        drawGrid();

        // 보드 클릭 이벤트 등록
        boardRoot.setOnMouseClicked(e -> {
            int c = (int) Math.round(e.getX() / CELL);
            int r = (int) Math.round(e.getY() / CELL);
            handleLocalClick(r, c);
        });

        // 말풍선 리스트, 턴 정보, 타이머 시작
        setupMessageList();
        updateTurnLabel();
        updateActivePlayerHighlight();
        startTurn();
    }

    /**
     * MatchSuccess / 카드 선택 화면에서 저장해둔 아바타 정보를 이용해
     * - top / bottom 프로필 이미지
     * - 선공/후공(mySign)
     * - 내 돌 / 상대 돌 이미지 경로
     * 를 세팅한다.
     */
    private void initAvatarsFromSession() {
        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();
        String[] avatars = MatchSession.getPlayerAvatars();

        if (players == null || avatars == null || players.length < 2 || me == null) {
            // 세션 정보가 없으면 FXML 기본 이미지 + 기본 돌 사용
            System.out.println("[GameBoard] WARN: MatchSession info missing.");
            return;
        }

        // 1) 내 인덱스 / 상대 인덱스 찾기
        int myIndex = 0;
        for (int i = 0; i < players.length; i++) {
            if (players[i].equals(me)) {
                myIndex = i;
                break;
            }
        }
        int oppIndex = (myIndex == 0) ? 1 : 0;

        // 2) 선공/후공 결정: players[0] 이 선공이라고 가정
        boolean iAmFirst = players[0].equals(me);
        mySign = iAmFirst ? 1 : -1;
        opponentSign = -mySign;

        // 3) 아바타 경로
        String myAvatarPath  = avatars[myIndex];
        String oppAvatarPath = avatars[oppIndex];

        // 4) 화면 배치: "항상 내 프로필이 아래!"
        bottomPlayerImage.setImage(
                new Image(getClass().getResource(myAvatarPath).toExternalForm())
        );
        topPlayerImage.setImage(
                new Image(getClass().getResource(oppAvatarPath).toExternalForm())
        );

        // 5) 돌 이미지 경로도 내 것 / 상대 것으로 분리 (sm_ 버전으로 변환)
        myStonePath = toStonePath(myAvatarPath);
        opponentStonePath = toStonePath(oppAvatarPath);

        // 혹시 다른 코드에서 top/bottomStonePath 를 쓰고 있을 수 있으니 맞춰 둠
        bottomStonePath = myStonePath;
        topStonePath = opponentStonePath;
    }

    /**
     * 아바타 이미지 경로("/images/user/user1.png")를
     * 돌 이미지 경로("/images/user/sm_user1.png")로 변환한다.
     */
    private String toStonePath(String avatarPath) {
        // avatarPath 예시: "/images/user/user1.png" 또는 "/images/user/sm_user1.png"
        if (avatarPath == null || avatarPath.isBlank()) {
            return "/images/user/sm_user1.png";
        }

        // 이미 sm_ 버전이면 그대로 사용
        if (avatarPath.contains("sm_user")) {
            return avatarPath;
        }

        int lastSlash = avatarPath.lastIndexOf('/');
        if (lastSlash < 0) {
            // 혹시 "user1.png" 처럼 파일명만 들어온 경우
            String file = avatarPath;
            if (!file.startsWith("sm_")) {
                file = "sm_" + file;           // user1.png -> sm_user1.png
            }
            return "/images/user/" + file;
        }

        String dir = avatarPath.substring(0, lastSlash + 1);  // "/images/user/"
        String file = avatarPath.substring(lastSlash + 1);    // "user1.png" 또는 "sm_user1.png"

        if (!file.startsWith("sm_")) {
            file = "sm_" + file;           // user1.png -> sm_user1.png
        }

        return dir + file;                 // "/images/user/sm_user1.png"
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
    /** 격자 그리기 */
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

    /** 로컬(나)에서 마우스로 보드를 클릭했을 때 처리 */
    private void handleLocalClick(int r, int c) {
        // ✅ 내 턴인지: current 가 mySign(1 or -1) 과 같은지만 본다
        boolean myTurn = (current == mySign);

        if (!myTurn) {
            return; // 내 턴 아니면 무시
        }

        if (!isInside(r, c) || board[r][c] != 0) {
            return; // 범위 밖 / 이미 돌이 있는 자리
        }

        // 실제로 돌 놓기 (공통)
        applyPlace(r, c);

        // 서버에 전송
        if (networkClient != null) {
            networkClient.sendPlace(r, c);
        }
    }

    /** 상대방으로부터 온 PLACE r c 처리 */
    public void onPlaceFromOpponent(int r, int c) {
        if (!isInside(r, c) || board[r][c] != 0) {
            return;
        }
        applyPlace(r, c);
    }

    /** 실제 돌 그리기 + 승리 검사 + 턴 전환 공통 로직 */
    private void applyPlace(int r, int c) {
        double cx = c * CELL;
        double cy = r * CELL;

        // 지금 두는 사람이 나인지 / 상대인지에 따라 돌 이미지 선택
        String stonePath = (current == mySign) ? myStonePath : opponentStonePath;

        // 🔥 안전장치: 경로가 잘못되면 기본 돌로 대체 (NPE 방지)
        java.net.URL url = getClass().getResource(stonePath);
        if (url == null) {
            System.out.println("[GameBoard] WARN: stone image not found: " + stonePath +
                    " -> fallback to /images/user/sm_user1.png");
            url = getClass().getResource("/images/user/sm_user1.png");
        }

        Image img = new Image(url.toExternalForm());
        ImageView stone = new ImageView(img);

        double stoneSize = CELL * 0.9;
        stone.setFitWidth(stoneSize);
        stone.setFitHeight(stoneSize);
        stone.setPreserveRatio(true);

        stone.setLayoutX(cx - stoneSize / 2);
        stone.setLayoutY(cy - stoneSize / 2);

        boardRoot.getChildren().add(stone);

        // 현재 턴의 플레이어(current)가 (r,c)에 둔 것
        board[r][c] = current;

        // ✅ 여기서 5목 승리 여부 검사
        if (checkWin(r, c, current)) {
            onGameOver(current);   // current가 이긴 사람의 sign(1 또는 -1)
            return;                // 더 이상 턴 전환 X
        }

        // 승리 아니면 턴 전환
        current *= -1;
        updateTurnLabel();
        updateActivePlayerHighlight();
        restartTimer();
    }

    /** 마지막에 (r,c)에 둔 sign(1 또는 -1)이 5목인지 검사 */
    private boolean checkWin(int r, int c, int sign) {
        // 가로
        if (countDirection(r, c, sign, 0, 1) + countDirection(r, c, sign, 0, -1) - 1 >= 5) return true;
        // 세로
        if (countDirection(r, c, sign, 1, 0) + countDirection(r, c, sign, -1, 0) - 1 >= 5) return true;
        // ↘ 대각선
        if (countDirection(r, c, sign, 1, 1) + countDirection(r, c, sign, -1, -1) - 1 >= 5) return true;
        // ↗ 대각선
        if (countDirection(r, c, sign, 1, -1) + countDirection(r, c, sign, -1, 1) - 1 >= 5) return true;

        return false;
    }

    /** (dr,dc) 방향으로 같은 sign이 몇 개 연속인지 센다 (자기 자신 포함) */
    private int countDirection(int r, int c, int sign, int dr, int dc) {
        int cnt = 0;
        int nr = r;
        int nc = c;

        while (isInside(nr, nc) && board[nr][nc] == sign) {
            cnt++;
            nr += dr;
            nc += dc;
        }
        return cnt;
    }

    /** 승패가 결정되었을 때 호출: winnerSign = 1(위) 또는 -1(아래) */
    private void onGameOver(int winnerSign) {
        // 더 이상 타이머 / 클릭 동작 X
        stopTimer();
        boardRoot.setOnMouseClicked(null);

        // 내가 이겼는지 여부
        boolean iWon = (winnerSign == mySign);

        // 결과 화면(모달 오버레이) 띄우기
        openResultScene(iWon);
    }

    /** 결과 화면(ResultView) FXML 로드 + ResultController에 데이터 전달 (모달 오버레이) */
    private void openResultScene(boolean iWon) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/result/ResultView.fxml")
            );
            Parent overlay = loader.load();
            ResultController controller = loader.getController();

            // 🔥 MatchSession에서 플레이어/아바타 정보 읽기
            String[] players = MatchSession.getPlayers();
            String[] avatars = MatchSession.getPlayerAvatars();
            String me = MatchSession.getMyNickname();

            String[][] ranking;

            if (players == null || avatars == null || players.length < 2 || me == null) {
                System.out.println("[GameBoard] WARN: cannot build ranking, MatchSession info missing.");
                // 그래도 화면은 띄워보자 (더미 데이터)
                ranking = new String[][]{
                        {"1", "Player1", "80", "/images/user/user1.png"},
                        {"2", "Player2", "40", "/images/user/user2.png"}
                };
            } else {
                // 내 인덱스 / 상대 인덱스
                int myIdx = 0;
                for (int i = 0; i < players.length; i++) {
                    if (players[i].equals(me)) {
                        myIdx = i;
                        break;
                    }
                }
                int oppIdx = (myIdx == 0) ? 1 : 0;

                // 점수: 이긴 사람 80, 진 사람 40
                ranking = new String[2][4];
                if (iWon) {
                    ranking[0] = new String[]{"1", players[myIdx], "80", avatars[myIdx]};
                    ranking[1] = new String[]{"2", players[oppIdx], "40", avatars[oppIdx]};
                } else {
                    ranking[0] = new String[]{"1", players[oppIdx], "80", avatars[oppIdx]};
                    ranking[1] = new String[]{"2", players[myIdx], "40", avatars[myIdx]};
                }
            }

            // 컨트롤러에 결과 데이터 세팅
            controller.showResult(iWon, ranking);

            // 🔹 GameBoard 중앙 StackPane 위에 모달 오버레이로 추가
            overlay.setMouseTransparent(false);   // 아래 클릭 막기
            centerStack.getChildren().add(overlay);

            // (보드는 이미 onGameOver에서 클릭 막았으므로 추가 조치는 선택 사항)
            // boardRoot.setMouseTransparent(true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 서버에서 "상대가 게임을 나갔다"는 이벤트를 받았을 때 호출 */
    public void onOpponentLeft() {
        System.out.println("[GameBoard] opponent left -> I win by default.");
        // 남아있는 내가 승리
        onGameOver(mySign);
    }

    private boolean isInside(int r, int c) {
        return r >= 0 && r < N && c >= 0 && c < N;
    }

    // ================== 아바타 하이라이트 / 턴 텍스트 ==================
    /** 위/아래 아바타 테두리로 현재 턴 강조 */
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

        boolean myTurn = (current == mySign);

        if (myTurn) {
            // ✅ 내 턴이면 아래(나)를 강조
            bottomPlayerContainer.setStyle(activeStyle);
            topPlayerContainer.setStyle(inactiveStyle);
        } else {
            topPlayerContainer.setStyle(activeStyle);
            bottomPlayerContainer.setStyle(inactiveStyle);
        }
    }

    /** 상단 텍스트로 "내 턴 / 상대 턴" 표시 */
    private void updateTurnLabel() {
        boolean myTurn = (current == mySign);

        if (myTurn) {
            turnLabel.setText("내 턴 (아래 유저)");
        } else {
            turnLabel.setText("상대 턴 (위 유저)");
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

    /** 내 말풍선 전송 */
    private void sendBalloon(String text) {
        messageSelectPane.setVisible(false);
        showMyBalloon(text);

        if (cheerSender != null) {
            cheerSender.sendCheer(text);
        }
    }

    private void showMyBalloon(String text) {
        // ✅ 나는 항상 아래
        showBalloonOn(bottomMessageBubble, bottomMessageLabel, text);
    }

    /** 상대방 말풍선 수신 */
    public void onCheerReceivedFromOpponent(String text) {
        // ✅ 상대는 항상 위
        showBalloonOn(topMessageBubble, topMessageLabel, text);
    }

    private void showBalloonOn(StackPane bubble, Label label, String text) {
        label.setText(text);
        bubble.setVisible(true);

        PauseTransition hide = new PauseTransition(Duration.seconds(2));
        hide.setOnFinished(e -> bubble.setVisible(false));
        hide.play();
    }

    // ================== 카드 선택 모달 / TimeLock / Swap / 카드 슬롯 ==================
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

    // ================== 네트워크 바인딩 ==================
    public interface NetworkClient {
        void sendCheer(String msg);
        void sendPlace(int row, int col);
    }

    private NetworkClient networkClient;

    /** GameIntroController에서 OmokClient와 연결해줄 때 호출 */
    public void bindNetwork(NetworkClient client) {
        this.networkClient = client;
        // 말풍선용 래핑 (기존 cheerSender 그대로 사용)
        this.cheerSender = client::sendCheer;
    }
}