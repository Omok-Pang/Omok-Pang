package com.omokpang.controller.game;

import com.omokpang.controller.effect.SwapSelectGuideController;
import com.omokpang.controller.effect.SharedStoneGuideController;
import com.omokpang.controller.effect.SharedStoneNoticeController;
import com.omokpang.controller.effect.BombGuideController;
import com.omokpang.controller.effect.BombNoticeController;
import com.omokpang.controller.effect.DoubleMoveNoticeController;
import com.omokpang.controller.effect.RemoveGuideController;
import com.omokpang.controller.effect.RemoveNoticeController;
import com.omokpang.controller.effect.ShieldNoticeController;
import com.omokpang.controller.effect.DefenseNoticeController;

import java.util.ArrayList;
import java.util.Collections;

import com.omokpang.domain.card.Card;
import com.omokpang.session.MatchSession;

import com.omokpang.controller.result.ResultController;
import javafx.scene.Parent;
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
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.Comparator;

/**
 * 역할: 실제 오목판 화면.
 *  - 위/아래(좌/우) 플레이어 아바타 표시
 *  - MatchSession에서 아바타/닉네임/카드 정보를 가져와 배치
 *  - 돌 두기 / 턴 전환 / 타이머 / 말풍선 / 카드 효과 처리
 *
 *  🔥 변경점: 턴은 서버가 관리한다.
 *   - 내 턴이 끝나면 TURN_END 를 서버로 보냄
 *   - 서버가 TURN <nickname> 을 브로드캐스트 → onTurnFromServer(...)에서 반영
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

    // ---- 내/상대 정보 ----
    /** 나는 선공(1)인지 후공(-1)인지 (first / second) */
    private int mySign = 1;
    /** 상대는 항상 나의 반대 */
    private int opponentSign = -1;

    /** 내가 선공인지 여부 (players[0] == me) */
    private boolean iAmFirst = false;

    // 플레이어 목록 / 내 닉네임 / 내 인덱스
    private String[] players;
    private String myNickname;
    private int myIndex = 0;

    // 🔥 팀 정보 (0팀, 1팀)
    private int[] playerTeam;           // 길이 = players.length, 값 = 0 또는 1
    private String[] stonePathOfTeam = new String[3]; // 팀별 돌 이미지 [1]=팀0, [2]=팀1

    // 내 돌 / 상대 돌 이미지 경로 (sm_ 아이콘)
    private String myStonePath;
    private String opponentStonePath;
    private String[] stonePathOfPlayer;   // 각 플레이어 돌 이미지 배열

    // 현재 턴을 가진 플레이어 닉네임(서버 기준)
    private String currentTurnNickname = null;
    // 이 클라이언트 기준: 지금이 내 턴인지 여부
    private boolean myTurn = false;

    // ================== Swap / SharedStone / Bomb 카드 관련 상태 ==================
    private SwapSelectGuideController swapGuideController;
    private boolean swapSelecting = false;
    private int[] swapMyPos = null;

    // SharedStone
    private boolean sharedStoneSelecting = false;
    private SharedStoneGuideController sharedStoneGuideController;

    // Bomb!!
    private boolean bombSelecting = false;
    private BombGuideController bombGuideController;

    // 한 턴에 남아 있는 수 (기본 1, DoubleMove 사용 시 2)
    private int movesLeftInCurrentTurn = 1;

    // Remove (상대 돌 1개 제거)
    private boolean removeSelecting = false;
    private RemoveGuideController removeGuideController;

    // Shield (자동 발동 방어 카드)
    private boolean hasShieldCard = false;

    // Shield 로 인해 공격 효과를 무시해야 하는지 플래그
    private boolean shieldBlockRemovePending = false;
    private boolean shieldBlockSwapPending = false;

    // Defense 카드
    private boolean defenseReady = false;             // DEFENSE 카드를 이번 턴에 활성화했는가

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

    // 좌/우 유저 말풍선 영역
    @FXML private StackPane leftMessageBubble;
    @FXML private Label leftMessageLabel;
    @FXML private StackPane rightMessageBubble;
    @FXML private Label rightMessageLabel;

    // 선택된 카드 아이콘 표시 영역 (오른쪽 아래)
    @FXML private HBox cardSlotBox;
    @FXML private Pane highlightPane;

    // 카드 선택 화면에서 전달받은 카드 두 장
    private List<Card> receivedCards;

    // ================== 보드 / 턴 관련 상수 & 상태 ==================
    private static final int N = 15;            // 보드 크기 (15 x 15)
    private static final double SIZE = 360;     // 보드 한 변 길이(px)
    private static final double CELL = SIZE / (N - 1); // 한 칸(격자 간격) 크기

    // 보드 상태: 0=빈칸, 1=선공 돌, -1=후공 돌
    private final int[][] board = new int[N][N];

    // 게임이 이미 끝났는지 여부 (카드 사용 후 중복 턴 전환 방지)
    private boolean gameEnded = false;

    // 돌 이미지 뷰 저장 (SharedStone 등으로 변경하기 위해)
    private final ImageView[][] stoneViews = new ImageView[N][N];

    // 공용돌(SharedStone) 여부 표시
    private final boolean[][] sharedStones = new boolean[N][N];

    // 현재 표시 중인 하이라이트 Rectangle 들
    private final List<Rectangle> bombHighlights = new ArrayList<>();

    // ================== 타이머 관련 ==================
    private static final int DEFAULT_TURN_SECONDS = 20; // 기본 턴 시간
    private static final int TIMELOCK_TURN_SECONDS = 3; // Time Lock 적용 시 턴 시간

    private Timeline timer;   // 1초마다 동작하는 타이머
    private int remain = DEFAULT_TURN_SECONDS;  // 남은 시간(초)

    /** Time Lock 카드로 인해 "내 다음 턴"이 3초 제한인지 여부 */
    private boolean timeLockNextTurn = false;

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

    /**
     * 4인 FFA 모드 레이아웃 설정.
     * - 항상 나는 아래
     * - 좌/우 아바타 활성화
     */
    public void configureForFourPlayers() {
        this.oneVsOne = false;
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
        if (leftMessageBubble != null) leftMessageBubble.setVisible(false);
        if (rightMessageBubble != null) rightMessageBubble.setVisible(false);

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

        // 말풍선 리스트
        setupMessageList();

        // 🔥 서버 턴 관리와 동기화: 처음 선공은 players[0]
        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();
        if (players != null && players.length >= 2 && me != null) {
            currentTurnNickname = players[0];        // 선공 닉네임
            // 초기 턴을 직접 세팅 (서버도 같은 상태를 내부적으로 유지)
            onTurnFromServer(currentTurnNickname);   // TURN players[0] 과 동일 처리
        } else {
            // 세션 정보가 없으면 일단 내 턴 아님
            myTurn = false;
            updateTurnLabel();
            updateActivePlayerHighlight();
            stopTimer();
            timerLabel.setText("");
        }
    }

    /**
     * MatchSuccess / 카드 선택 화면에서 저장해둔 아바타 정보를 이용해
     * - top / bottom / left / right 프로필 이미지
     * - 내 인덱스 / mySign
     * - 플레이어별 돌 이미지 경로(stonePathOfPlayer)
     * 를 세팅한다.
     */
    private void initAvatarsFromSession() {
        String[] players = MatchSession.getPlayers();
        String me = MatchSession.getMyNickname();
        String[] avatars = MatchSession.getPlayerAvatars();

        this.players = players;
        this.myNickname = me;

        this.playerTeam = MatchSession.getPlayerTeam();

        if (players == null || avatars == null || players.length < 2 || me == null) {
            System.out.println("[GameBoard] WARN: MatchSession info missing.");
            return;
        }

        // 1) 플레이어별 돌 이미지 경로(sm_ 아바타) 세팅
        stonePathOfPlayer = new String[players.length];
        for (int i = 0; i < players.length; i++) {
            stonePathOfPlayer[i] = toStonePath(avatars[i]);
        }

        // 2) 내 인덱스 찾기
        int myIdx = 0;
        for (int i = 0; i < players.length; i++) {
            if (players[i].equals(me)) {
                myIdx = i;
                break;
            }
        }
        this.myIndex = myIdx;
        // 🔥 보드 위 내 sign은 항상 (내 인덱스 + 1)
        this.mySign = myIdx + 1;

        if (players.length == 2) {
            // ===== 1:1 배치 =====
            int oppIndex = (myIdx == 0) ? 1 : 0;

            String myAvatarPath  = avatars[myIdx];
            String oppAvatarPath = avatars[oppIndex];

            bottomPlayerImage.setImage(
                    new Image(getClass().getResource(myAvatarPath).toExternalForm())
            );
            topPlayerImage.setImage(
                    new Image(getClass().getResource(oppAvatarPath).toExternalForm())
            );

            // 돌 경로는 이미 stonePathOfPlayer에 세팅되어 있음
            bottomStonePath = stonePathOfPlayer[myIdx];
            topStonePath = stonePathOfPlayer[oppIndex];

            myStonePath = bottomStonePath;
            opponentStonePath = topStonePath;

            // 팀 정보 없으면 0 vs 1로 기본 세팅 (팀전이 아니어도 무방)
            if (playerTeam == null || playerTeam.length != 2) {
                playerTeam = new int[2];
                playerTeam[0] = 0;
                playerTeam[1] = 1;
            }

            // 기존 코드 호환용 (실제 로직에서는 opponentSign을 쓰지 않게 변경했지만 남겨둠)
            opponentSign = oppIndex + 1;

        } else if (players.length == 4) {
            // ===== 4인 배치 =====
            // 내 기준: 아래(나) -> 왼쪽 -> 위 -> 오른쪽 (시계 방향)
            int leftIdx  = (myIdx + 1) % 4;
            int topIdx   = (myIdx + 2) % 4;
            int rightIdx = (myIdx + 3) % 4;

            String myAvatarPath    = avatars[myIdx];
            String leftAvatarPath  = avatars[leftIdx];
            String topAvatarPath   = avatars[topIdx];
            String rightAvatarPath = avatars[rightIdx];

            bottomPlayerImage.setImage(
                    new Image(getClass().getResource(myAvatarPath).toExternalForm())
            );
            topPlayerImage.setImage(
                    new Image(getClass().getResource(topAvatarPath).toExternalForm())
            );
            leftPlayerImage.setImage(
                    new Image(getClass().getResource(leftAvatarPath).toExternalForm())
            );
            rightPlayerImage.setImage(
                    new Image(getClass().getResource(rightAvatarPath).toExternalForm())
            );

            // 팀 정보 없으면 0,2 vs 1,3 으로 강제 (A팀 / B팀)
            if (playerTeam == null || playerTeam.length != 4) {
                playerTeam = new int[4];
                playerTeam[0] = 0; // A팀
                playerTeam[1] = 1; // B팀
                playerTeam[2] = 0; // A팀
                playerTeam[3] = 1; // B팀
            }

            // 4인전에서도 돌 이미지는 "플레이어별 sm_ 아바타" 그대로 사용한다.
            bottomStonePath = stonePathOfPlayer[myIdx];
            topStonePath = stonePathOfPlayer[topIdx];
        }
    }

    /** 서버에서 CHEER <fromNickname> <text> 를 받았을 때 호출 */
    public void onCheerReceived(String fromNickname, String text) {
        if (fromNickname == null || text == null) return;

        // players 정보가 없으면 기존 1:1처럼 처리
        if (players == null || players.length == 0) {
            onCheerReceivedFromOpponent(text);
            return;
        }

        // 보낸 사람 인덱스 찾기
        int idx = -1;
        for (int i = 0; i < players.length; i++) {
            if (players[i].equals(fromNickname)) {
                idx = i;
                break;
            }
        }

        if (idx == -1) {
            // 누군지 모르겠으면 일단 위쪽에
            onCheerReceivedFromOpponent(text);
            return;
        }

        // 내가 보낸 말이면 내 말풍선
        if (idx == myIndex) {
            showMyBalloon(text);
            return;
        }

        // 1:1 모드
        if (players.length == 2) {
            showBalloonOn(topMessageBubble, topMessageLabel, text);
            return;
        }

        // 4인 모드: 내 기준으로 어느 자리인지 계산
        if (players.length == 4) {
            int leftIdx  = (myIndex + 1) % 4;
            int topIdx   = (myIndex + 2) % 4;
            int rightIdx = (myIndex + 3) % 4;

            if (idx == leftIdx) {
                showBalloonOn(leftMessageBubble, leftMessageLabel, text);
            } else if (idx == topIdx) {
                showBalloonOn(topMessageBubble, topMessageLabel, text);
            } else if (idx == rightIdx) {
                showBalloonOn(rightMessageBubble, rightMessageLabel, text);
            } else {
                // 혹시 모를 예외: 그냥 위쪽에 뿌려줌
                showBalloonOn(topMessageBubble, topMessageLabel, text);
            }
        }
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

    /** 지금이 내 턴인지 여부 (서버 기준 턴 + 내 닉네임 비교 결과) */
    private boolean isMyTurn() {
        return myTurn;
    }

    /** 로컬(나)에서 마우스로 보드를 클릭했을 때 처리 */
    private void handleLocalClick(int r, int c) {

        // 🔥 게임이 이미 끝났으면 아무 동작도 하지 않음
        if (gameEnded) {
            return;
        }

        // ✅ Swap 선택 모드인 경우: 내 돌 → 상대 돌 순서로 선택
        if (swapSelecting) {
            handleSwapSelectClick(r, c);
            return;
        }

        // ✅ Bomb 선택 모드인 경우: 3x3 제거용 클릭으로 사용
        if (bombSelecting) {
            handleBombTargetClick(r, c);
            return;
        }

        // ✅ SharedStone 선택 모드인 경우: 돌 두기 대신 "상대 돌 선택"으로 사용
        if (sharedStoneSelecting) {
            handleSharedStoneTargetClick(r, c);
            return;
        }

        // ✅ Remove 선택 모드 (상대 돌 1개 제거)
        if (removeSelecting) {
            handleRemoveTargetClick(r, c);
            return;
        }

        // ✅ 일반 돌 두기: 내 턴인지 확인
        if (!isMyTurn()) {
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

        // 승리로 게임이 끝났다면 더 이상 처리 X
        if (gameEnded) return;

        // 한 턴에 남은 수가 없으면 (기본 1번, DoubleMove면 2번) → 내 턴 종료
        if (movesLeftInCurrentTurn <= 0 && !gameEnded) {
            endMyTurn();
        }
    }

    /** 상대방으로부터 온 PLACE r c 처리 */
    public void onPlaceFromOpponent(int r, int c) {
        if (!isInside(r, c) || board[r][c] != 0) {
            return;
        }
        applyPlace(r, c);
        // 상대가 둔 수에 대해서는 이쪽에서 TURN_END 를 보내지 않는다.
    }

    /**
     * 실제 돌 그리기 + 승리 검사 + 한 턴에 둘 수 있는 수(movesLeftInCurrentTurn) 차감
     */
    private void applyPlace(int r, int c) {
        double cx = c * CELL;
        double cy = r * CELL;

        String me = MatchSession.getMyNickname();
        // 현재 턴인 사람 인덱스 찾기
        int currentIdx = -1;
        for (int i = 0; i < players.length; i++) {
            if (players[i].equals(currentTurnNickname)) {
                currentIdx = i;
                break;
            }
        }

        // 절대 sign = index + 1 (1~4)
        int sign = currentIdx + 1;

        // 돌 이미지 배열에서 현재 플레이어의 이미지 경로 가져오기
        String stonePath = stonePathOfPlayer[currentIdx];


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

        board[r][c] = sign;
        stoneViews[r][c] = stone;

        // 팀전이면 "팀 기준" 승리, 아니면 기존 개인 기준
        if (isTeamMode2v2()) {
            int teamId = playerTeam[currentIdx];   // 현재 돌을 둔 플레이어의 팀

            if (checkTeamWin(r, c, teamId)) {
                // 팀이 이겼지만, 기존 onGameOver는 "플레이어 sign"을 받으니까
                // 우선 이 돌을 둔 플레이어를 대표로 넘겨줌
                onGameOver(sign);
                return;
            }
        } else {
            if (checkWin(r, c, sign)) {
                onGameOver(sign);
                return;
            }
        }

        // 한 턴에 둘 수 있는 수 감소 (기본 1, DoubleMove 시 2)
        movesLeftInCurrentTurn--;
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

        while (isStoneForSign(nr, nc, sign)) {
            cnt++;
            nr += dr;
            nc += dc;
        }
        return cnt;
    }

    /** 승패가 결정되었을 때 호출: winnerSign = 1..N (플레이어 인덱스 + 1) */
    private void onGameOver(int winnerSign) {
        // 이미 끝난 뒤에 또 호출되는 것 방지
        if (gameEnded) return;
        gameEnded = true;

        // 🔒 더 이상 타이머 / 클릭 동작 X
        stopTimer();
        boardRoot.setOnMouseClicked(null);

        boolean iWon;

        if (isTeamMode2v2()) {
            int winnerIdx = winnerSign - 1;
            int winnerTeam = playerTeam[winnerIdx];
            int myTeam = playerTeam[myIndex];

            iWon = (winnerTeam == myTeam);   // 같은 팀이면 둘 다 승리 처리
        } else {
            iWon = (winnerSign == (myIndex + 1));
        }

        // 🔥 여기서 바로 모달을 띄우지 않고,
        //    0.5초 정도 딜레이 후에 결과 모달 오픈
        boolean finalIWon = iWon;
        PauseTransition delay = new PauseTransition(Duration.millis(500));
        delay.setOnFinished(e -> openResultScene(winnerSign, finalIWon));
        delay.play();
    }

    /** 내 턴을 종료하고 서버에 TURN_END 전송 (서버가 턴을 넘긴다) */
    private void endMyTurn() {
        if (!isMyTurn()) return;
        if (gameEnded) return;

        System.out.println("[GameBoard] endMyTurn() 호출 - TURN_END 전송");

        stopTimer();
        myTurn = false;
        updateTurnLabel();
        updateActivePlayerHighlight();

        if (networkClient != null) {
            networkClient.sendTurnEnd();
        }
    }

    /**
     * 결과 화면(ResultView) FXML 로드 + ResultController에 데이터 전달 (모달 오버레이)
     * @param winnerSign 승자 sign (1..N, players 인덱스 + 1)
     * @param iWon       이 클라이언트가 이겼는지 여부
     */
    private void openResultScene(int winnerSign, boolean iWon) {
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
                int n = players.length;
                // 임시 리스트에 담았다가 rank 기준으로 정렬
                java.util.List<String[]> list = new ArrayList<>();

                // 🔥 winnerSign = winnerIdx + 1 이라는 전제
                int winnerIdx = winnerSign - 1;
                if (winnerIdx < 0 || winnerIdx >= n) {
                    winnerIdx = 0;
                }

                for (int i = 0; i < n; i++) {

                    int rank;
                    String score;

                    if (n == 2) {
                        // ✅ 2인: 1등 80, 2등 40
                        if (i == winnerIdx) {
                            rank = 1;
                            score = "80";
                        } else {
                            rank = 2;
                            score = "40";
                        }
                    } else if (n == 4 && isTeamMode2v2()) {
                        int winnerTeam = playerTeam[winnerIdx];
                        int myTeam = playerTeam[i];

                        if (playerTeam[i] == winnerTeam) {
                            rank = 1;
                            score = "80";
                        } else {
                            rank = 2;
                            score = "40";
                        }
                    } else {
                        // 그 외 인원수는 일단 0점 처리 (필요 시 규칙 추가)
                        rank = (i == winnerIdx) ? 1 : 2;
                        score = (i == winnerIdx) ? "80" : "0";
                    }

                    list.add(new String[]{
                            String.valueOf(rank), // 순위
                            players[i],           // 닉네임
                            score,                // 점수
                            avatars[i]            // 아바타 경로
                    });
                }

                // 🔥 rank 기준 오름차순 정렬 → 1등이 항상 첫 번째에 오도록
                list.sort(Comparator.comparingInt(a -> Integer.parseInt(a[0])));

                ranking = list.toArray(new String[0][0]);
            }

            // 컨트롤러에 결과 데이터 세팅
            controller.showResult(iWon, ranking);

            // 🔹 GameBoard 중앙 StackPane 위에 모달 오버레이로 추가
            overlay.setMouseTransparent(false);   // 아래 클릭 막기
            centerStack.getChildren().add(overlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 서버에서 "상대가 게임을 나갔다"는 이벤트를 받았을 때 호출 */
    public void onOpponentLeft() {
        System.out.println("[GameBoard] opponent left -> I win by default.");
        // 남아있는 내가 승리
        onGameOver(myIndex + 1);
    }

    private boolean isInside(int r, int c) {
        return r >= 0 && r < N && c >= 0 && c < N;
    }

    // ================== 아바타 하이라이트 / 턴 텍스트 ==================
    /** 위/아래/좌/우 아바타 테두리로 현재 턴 강조 */
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

        // 기본은 전부 inactive
        bottomPlayerContainer.setStyle(inactiveStyle);
        topPlayerContainer.setStyle(inactiveStyle);
        if (leftPlayerContainer != null) leftPlayerContainer.setStyle(inactiveStyle);
        if (rightPlayerContainer != null) rightPlayerContainer.setStyle(inactiveStyle);

        if (currentTurnNickname == null || players == null || players.length == 0) {
            return;
        }

        int curIdx = -1;
        for (int i = 0; i < players.length; i++) {
            if (players[i].equals(currentTurnNickname)) {
                curIdx = i;
                break;
            }
        }
        if (curIdx == -1) return;

        if (players.length == 2) {
            // 1:1 – 내가 아래, 다른 사람은 위
            if (curIdx == myIndex) {
                bottomPlayerContainer.setStyle(activeStyle);
            } else {
                topPlayerContainer.setStyle(activeStyle);
            }
        } else if (players.length == 4) {
            int leftIdx  = (myIndex + 1) % 4;
            int topIdx   = (myIndex + 2) % 4;
            int rightIdx = (myIndex + 3) % 4;

            if (curIdx == myIndex) {
                bottomPlayerContainer.setStyle(activeStyle);
            } else if (curIdx == leftIdx && leftPlayerContainer != null) {
                leftPlayerContainer.setStyle(activeStyle);
            } else if (curIdx == topIdx) {
                topPlayerContainer.setStyle(activeStyle);
            } else if (curIdx == rightIdx && rightPlayerContainer != null) {
                rightPlayerContainer.setStyle(activeStyle);
            }
        }
    }

    /** 상단 텍스트로 "내 턴 / 상대 턴" 표시 */
    private void updateTurnLabel() {
        if (currentTurnNickname == null) {
            turnLabel.setText("");
            return;
        }

        boolean myTurnNow = isMyTurn();

        // 플레이어 번호 기반 출력 (4인 포함)
        if (players != null && players.length >= 2) {
            int curIdx = -1;
            for (int i = 0; i < players.length; i++) {
                if (players[i].equals(currentTurnNickname)) {
                    curIdx = i;
                    break;
                }
            }

            if (curIdx != -1) {
                int num = curIdx + 1; // 1~N
                if (myTurnNow) {
                    turnLabel.setText("내 턴 (" + num + "번 플레이어)");
                } else {
                    turnLabel.setText("현재 턴: " + num + "번 플레이어");
                }
                return;
            }
        }

        // fallback (기존 1:1 문구)
        if (myTurnNow) {
            turnLabel.setText("내 턴 (아래 유저)");
        } else {
            turnLabel.setText("상대 턴 (위 유저)");
        }
    }

    // ================== 턴 타이머 로직 ==================
    /** 서버로부터 "TURN <nickname>" 을 받았을 때 호출 */
    public void onTurnFromServer(String nickname) {
        System.out.println("[GameBoard] onTurnFromServer: " + nickname);
        this.currentTurnNickname = nickname;

        String me = MatchSession.getMyNickname();
        this.myTurn = (me != null && me.equals(nickname));

        if (myTurn) {
            // 내 턴 시작: 타이머 / movesLeft 초기화

            // 🔥 지난 턴에 사용했던 Defense 버프는 상대 턴 동안만 유효,
            // 내 턴이 다시 돌아오면 소멸 (상대가 공격 안 해서 허공에 버려진 상태)
            if (defenseReady) {
                defenseReady = false;
                System.out.println("[GameBoard] Defense 버프가 사용되지 않고 소멸되었습니다.");
            }

            startTurn();
        } else {
            // 상대 턴: 타이머 정지
            stopTimer();
            movesLeftInCurrentTurn = 1;
            timerLabel.setText("");

            cancelAllCardSelectionModes();
        }

        updateTurnLabel();
        updateActivePlayerHighlight();
    }

    /** 턴이 넘어갈 때 / 게임 끝날 때 카드 선택 모드들 강제 취소 */
    private void cancelAllCardSelectionModes() {
        // Swap
        if (swapSelecting) {
            swapSelecting = false;
            swapMyPos = null;
            if (swapGuideController != null) {
                swapGuideController.close();
                swapGuideController = null;
            }
        }

        // SharedStone
        if (sharedStoneSelecting) {
            sharedStoneSelecting = false;
            if (sharedStoneGuideController != null) {
                sharedStoneGuideController.close();
                sharedStoneGuideController = null;
            }
        }

        // Bomb
        if (bombSelecting) {
            bombSelecting = false;
            if (bombGuideController != null) {
                bombGuideController.close();
                bombGuideController = null;
            }
        }

        // Remove
        if (removeSelecting) {
            removeSelecting = false;
            if (removeGuideController != null) {
                removeGuideController.close();
                removeGuideController = null;
            }
        }
    }

    /** 내 턴 시작 (서버 TURN 메시지 기준) */
    private void startTurn() {
        int seconds = DEFAULT_TURN_SECONDS;

        // Time Lock 카드로 인해 "이번 내 턴"이 3초 제한이면
        if (timeLockNextTurn) {
            seconds = TIMELOCK_TURN_SECONDS;
            timeLockNextTurn = false;  // 한 번만 적용
        }

        movesLeftInCurrentTurn = 1; // 기본 1수 (DoubleMove 카드 사용 시 2로 변경)

        startTurnWithSeconds(seconds);
    }

    /** Time Lock 카드 사용 (내가 카드 선택했을 때 호출) */
    private void useTimeLockCard() {
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 Time Lock 카드를 사용할 수 없습니다.");
            return;
        }

        System.out.println("[GameBoard] Time Lock 카드 사용!");

        // 다음 턴에 "상대"의 제한시간을 3초로 줄인다.
        // → 상대 클라이언트에서 onTimeLockStartFromOpponent()에서 timeLockNextTurn = true 로 세팅
        if (networkClient != null) {
            networkClient.sendTimeLockStart();
        }

        // 이 카드를 사용하면 내 턴은 종료
        if (!gameEnded) {
            endMyTurn();
        }
    }

    private void startTurnWithSeconds(int seconds) {
        stopTimer();

        remain = seconds;
        timerLabel.setText(remain + "초");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remain--;
            timerLabel.setText(remain + "초");

            if (remain <= 0) {
                timer.stop();
                timerLabel.setText("시간 초과");

                // 시간 초과 → 남은 수는 0으로 간주하고 내 턴 종료
                movesLeftInCurrentTurn = 0;

                if (!gameEnded && isMyTurn()) {
                    endMyTurn();
                }
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.playFromStart();
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

    // ================== 카드 모달 오픈 ==================

    /**
     * FXML에서 카드 슬롯(HBox)을 클릭했을 때 호출되는 메서드.
     * - 오른쪽 아래 카드 영역 클릭 → 카드 사용 모달 띄우기
     */
    @FXML
    private void handleOpenCardModal() {
        // ✅ 내 턴이 아니면 아무 일도 안 하도록
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 카드 사용이 불가합니다.");
            return;
        }
        openCardUseModal();
    }

    /**
     * 실제로 CardUseModal.fxml을 로드하여 centerStack 위에 오버레이로 올린다.
     */
    private void openCardUseModal() {
        // 아직 받은 카드가 없으면 아무 것도 안 함
        if (receivedCards == null || receivedCards.isEmpty()) {
            System.out.println("[GameBoard] 카드가 없어 모달을 띄우지 않습니다.");
            return;
        }

        // SHIELD 를 제외한 선택 가능한 카드만 모달에 넘김
        List<Card> usableCards = getUsableCardsForModal();
        if (usableCards.isEmpty()) {
            System.out.println("[GameBoard] 선택 가능한 카드가 없어 모달을 띄우지 않습니다.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/game/CardUseModal.fxml")
            );
            StackPane modalRoot = loader.load();

            CardUseModalController controller = loader.getController();
            // 1) 내가 가진 카드 목록 전달 (SHIELD 제외)
            controller.setCards(usableCards);
            // 2) 어떤 카드를 골랐는지 콜백으로 전달
            controller.setOnCardSelected(this::onCardSelectedFromModal);

            modalRoot.setMouseTransparent(false);   // 아래 클릭 막기
            centerStack.getChildren().add(modalRoot);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 카드 사용 모달에서 카드 하나를 선택했을 때 호출되는 콜백.
     * - 여기서 카드 타입별로 효과 처리.
     */
    private void onCardSelectedFromModal(Card selectedCard) {
        if (selectedCard == null) return;

        // ✅ 혹시 모를 동기화 이슈 대비: 내 턴이 아니면 효과/제거 둘 다 하지 않는다.
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 선택된 카드 효과를 적용하지 않습니다.");
            return;
        }

        System.out.println("[GameBoard] 카드 선택됨: " + selectedCard.getName());

        try {
            switch (selectedCard.getType()) {
                case SHARED_STONE -> useSharedStoneCard();
                case BOMB         -> useBombCard();
                case TIME_LOCK    -> useTimeLockCard();
                case SWAP         -> useSwapCard();
                case DOUBLE_MOVE  -> useDoubleMoveCard();
                case REMOVE       -> useRemoveCard();
                case SHIELD       -> hasShieldCard = true;
                case DEFENSE      -> useDefenseCard();
                default           -> System.out.println("[GameBoard] 아직 구현되지 않은 카드 타입: " + selectedCard.getType());
            }
        } catch (Exception e) {
            System.out.println("[GameBoard] 카드 타입 처리 중 오류: " + e.getMessage());
        }

        // ✅ 여기까지 왔다는 건 "내 턴 + 카드 효과 실행"인 경우만
        if (receivedCards != null) {
            receivedCards.remove(selectedCard);
            setReceivedCards(receivedCards);
        }
    }

    // ================== SharedStone 카드 로직 ==================

    /**
     * SharedStone 카드 사용 시작 (내가 카드 선택했을 때 호출).
     * - 서버에 "SharedStone 시작" 알림
     * - SharedStone 안내 오버레이 + 상대 돌 선택 모드 진입
     */
    private void useSharedStoneCard() {
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 SharedStone 카드를 사용할 수 없습니다.");
            return;
        }

        System.out.println("[GameBoard] SharedStone 카드 사용!");

        // 서버에 "SharedStone 사용 시작" 알림
        if (networkClient != null) {
            networkClient.sendSharedStoneStart();
        }

        // 가이드 오버레이 + 선택 모드 시작
        enterSharedStoneSelectMode();
    }

    /**
     * SharedStone 선택 모드 진입.
     * - SharedStoneGuide.fxml 오버레이를 centerStack 위에 올리고
     * - 사용자가 상대 돌을 클릭하면 콜백으로 (r,c) 전달.
     */
    private void enterSharedStoneSelectMode() {
        sharedStoneSelecting = true;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/SharedStoneGuide.fxml")
            );
            StackPane overlay = loader.load();
            sharedStoneGuideController = loader.getController();

            // ✅ 안내만 하고, 실제 클릭은 아래(boardRoot)로 전달되게
            overlay.setMouseTransparent(true);

            sharedStoneGuideController.setOnStoneSelected((row, col) -> {
                onSharedStoneTargetChosenByMe(row, col);
            });

            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * SharedStone 선택 모드에서 보드를 클릭했을 때 동작.
     * - 상대 팀 돌만 선택 가능.
     */
    private void handleSharedStoneTargetClick(int r, int c) {
        if (!isInside(r, c)) return;

        int cellSign = board[r][c];

        if (cellSign == 0) {
            System.out.println("[GameBoard] SharedStone: 빈 칸을 클릭했습니다.");
            return;
        }

        // 팀 정보가 있으면: 같은 팀 돌은 선택 불가
        if (playerTeam != null && players != null
                && playerTeam.length == players.length) {

            int targetIdx = cellSign - 1;
            if (targetIdx < 0 || targetIdx >= playerTeam.length) {
                System.out.println("[GameBoard] SharedStone: 잘못된 sign 값 " + cellSign);
                return;
            }

            if (playerTeam[targetIdx] == playerTeam[myIndex]) {
                System.out.println("[GameBoard] SharedStone: 같은 팀 돌은 선택할 수 없습니다.");
                return;
            }
        } else {
            // 팀 정보가 없으면: 내 돌만 아니면 상대 돌 취급
            int mySignNow = myIndex + 1;
            if (cellSign == mySignNow) {
                System.out.println("[GameBoard] SharedStone: 내 돌을 선택했습니다.");
                return;
            }
        }

        if (sharedStoneGuideController != null) {
            sharedStoneGuideController.notifyStoneSelected(r, c);
        } else {
            onSharedStoneTargetChosenByMe(r, c);
        }
    }

    // ================== Bomb!! 카드 로직 ==================

    /** Bomb!! 카드 사용 시작 (내가 선택했을 때 호출) */
    private void useBombCard() {
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 Bomb 카드를 사용할 수 없습니다.");
            return;
        }

        System.out.println("[GameBoard] Bomb!! 카드 사용!");

        // 서버에 시작 알림
        if (networkClient != null) {
            networkClient.sendBombStart();
        }

        enterBombSelectMode();
    }

    /** 3×3 제거 구역 선택 모드 진입 */
    private void enterBombSelectMode() {
        bombSelecting = true;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/BombGuide.fxml")
            );
            StackPane overlay = loader.load();
            bombGuideController = loader.getController();

            bombGuideController.setOnAreaSelected((row, col) -> {
                onBombAreaChosenByMe(row, col);
            });

            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 🔥 마우스 움직일 때 3x3 하이라이트 업데이트
        boardRoot.setOnMouseMoved(e -> {
            int c = (int)Math.round(e.getX() / CELL);
            int r = (int)Math.round(e.getY() / CELL);
            updateBombHighlight(r, c);
        });

    }

    private void clearBombHighlight() {
        for (Rectangle rect : bombHighlights) {
            highlightPane.getChildren().remove(rect);
        }
        bombHighlights.clear();
    }

    private void updateBombHighlight(int centerR, int centerC) {

        if (highlightPane == null) return;

        clearBombHighlight();

        if (!bombSelecting) return;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = centerR + dr;
                int c = centerC + dc;
                if (!isInside(r, c)) continue;

                Rectangle rect = new Rectangle(CELL, CELL);
                rect.setStroke(Color.RED);
                rect.setStrokeWidth(2);
                rect.setFill(Color.color(1,0,0,0.15)); // 반투명 붉은색
                rect.setLayoutX(c * CELL - CELL/2);
                rect.setLayoutY(r * CELL - CELL/2);

                bombHighlights.add(rect);
                highlightPane.getChildren().add(rect);
            }
        }
    }

    /** 개별 돌 폭발 이펙트 (2초 후 자동 제거) */
    private void showSmallExplosionAt(int r, int c) {
        Image explosion = new Image(
                getClass().getResource("/images/effects/bomb_small.png").toExternalForm()
        );

        ImageView iv = new ImageView(explosion);
        iv.setFitWidth(48);   // 원하는 크기 (조절 가능)
        iv.setFitHeight(48);
        iv.setPreserveRatio(true);

        double cx = c * CELL;
        double cy = r * CELL;

        iv.setLayoutX(cx - 24);
        iv.setLayoutY(cy - 24);

        centerStack.getChildren().add(iv);

        // 2초 뒤 제거
        PauseTransition pt = new PauseTransition(Duration.seconds(2));
        pt.setOnFinished(e -> centerStack.getChildren().remove(iv));
        pt.play();
    }


    /** Bomb 선택 모드에서 보드를 클릭했을 때 */
    private void handleBombTargetClick(int r, int c) {
        if (!isInside(r, c)) return;

        if (bombGuideController != null) {
            bombGuideController.notifyAreaSelected(r, c);
        } else {
            onBombAreaChosenByMe(r, c);
        }
    }

    /** 내가 최종 3×3 중심 좌표를 고른 경우 */
    private void onBombAreaChosenByMe(int r, int c) {
        bombSelecting = false;

        applyBombArea(r, c);

        if (networkClient != null) {
            networkClient.sendBombTarget(r, c);
        }

        if (!gameEnded) {
            endMyTurn();
        }
    }

    /**
     * (r,c)를 중심으로 하는 3×3 영역의 돌을 모두 제거한다.
     *  - 최소 0개 ~ 최대 9개 제거
     */
    private void applyBombArea(int centerR, int centerC) {
        int removed = 0;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                int r = centerR + dr;
                int c = centerC + dc;

                if (!isInside(r, c)) continue;
                if (board[r][c] == 0) continue;

                ImageView stone = stoneViews[r][c];
                if (stone != null) {
                    // 🔥 작은 폭발 효과
                    showSmallExplosionAt(r, c);
                    boardRoot.getChildren().remove(stone);
                }

                board[r][c] = 0;
                stoneViews[r][c] = null;
                sharedStones[r][c] = false;
                removed++;
            }
        }

        System.out.println("[GameBoard] Bomb!! 적용: " + removed + "개 제거 (center=" + centerR + "," + centerC + ")");
    }

    /** 서버에서 '상대가 Bomb!! 카드를 사용했다' 알림을 받았을 때 */
    public void onBombStartFromOpponent() {
        System.out.println("[GameBoard] 상대가 Bomb!! 카드를 사용했습니다.");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/BombNotice.fxml")
            );
            StackPane overlay = loader.load();
            BombNoticeController controller = loader.getController();
            // 별도 데이터 전달 없음

            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** 서버에서 'BOMB_TARGET r c' 를 받았을 때 */
    public void onBombTargetFromOpponent(int r, int c) {
        System.out.println("[GameBoard] Bomb!! 타겟 좌표 수신: (" + r + "," + c + ")");
        applyBombArea(r, c);
        // 턴 전환은 서버의 TURN 메시지로 처리
    }

    /** 해당 좌표의 돌이 주어진 sign(1 또는 -1)의 연속에 포함되는지 여부 */
    private boolean isStoneForSign(int r, int c, int sign) {
        if (!isInside(r, c)) return false;

        // 원래 그 플레이어 돌
        if (board[r][c] == sign) return true;

        // 공용돌이면 양쪽 다 자신의 돌로 인정
        if (sharedStones[r][c]) return true;

        return false;
    }

    /** (r,c)가 teamId(0 또는 1)의 돌(또는 공용돌)인지 여부 */
    private boolean isStoneForTeam(int r, int c, int teamId) {
        if (!isInside(r, c)) return false;

        // 공용돌이면 어느 팀에게나 자신의 돌로 인정
        if (sharedStones[r][c]) return true;

        int sign = board[r][c];
        if (sign == 0) return false;

        // sign -> 플레이어 인덱스(0~3) -> 팀 번호
        int idx = sign - 1;
        if (playerTeam == null || idx < 0 || idx >= playerTeam.length) return false;

        return playerTeam[idx] == teamId;
    }

    /** (dr,dc) 방향으로 같은 팀 돌이 몇 개 연속인지 센다 (자기 자신 포함) */
    private int countDirectionForTeam(int r, int c, int teamId, int dr, int dc) {
        int cnt = 0;
        int nr = r;
        int nc = c;

        while (isStoneForTeam(nr, nc, teamId)) {
            cnt++;
            nr += dr;
            nc += dc;
        }
        return cnt;
    }

    /** 마지막에 (r,c)에 둔 teamId(0 또는 1)가 5목인지 검사 */
    private boolean checkTeamWin(int r, int c, int teamId) {
        // 가로
        if (countDirectionForTeam(r, c, teamId, 0, 1)
                + countDirectionForTeam(r, c, teamId, 0, -1) - 1 >= 5) return true;
        // 세로
        if (countDirectionForTeam(r, c, teamId, 1, 0)
                + countDirectionForTeam(r, c, teamId, -1, 0) - 1 >= 5) return true;
        // ↘ 대각선
        if (countDirectionForTeam(r, c, teamId, 1, 1)
                + countDirectionForTeam(r, c, teamId, -1, -1) - 1 >= 5) return true;
        // ↗ 대각선
        if (countDirectionForTeam(r, c, teamId, 1, -1)
                + countDirectionForTeam(r, c, teamId, -1, 1) - 1 >= 5) return true;

        return false;
    }

    /**
     * 내가 SharedStone 타겟 좌표(r,c)를 최종 선택했을 때 호출.
     * - SharedStone 효과를 내 보드에 적용
     * - 서버에 (r,c) 전송
     * - 이 턴은 "카드 사용"으로 끝 → 턴 종료
     */
    private void onSharedStoneTargetChosenByMe(int r, int c) {
        sharedStoneSelecting = false;

        // 실제 공용돌 적용 (여기서 승리하면 onGameOver에서 gameEnded = true)
        applySharedStoneAt(r, c);

        // 서버에 좌표 전송 (상대 보드도 동일하게 변경)
        if (networkClient != null) {
            networkClient.sendSharedStoneTarget(r, c);
        }

        // 이미 승리해서 게임이 끝난 경우에는 턴 종료 X
        if (!gameEnded) {
            endMyTurn();
        }
    }

    /**
     * (r,c)에 이미 놓인 돌을 "공용돌" 이미지로 변경하고, sharedStones 플래그를 세팅.
     */
    private void applySharedStoneAt(int r, int c) {
        if (!isInside(r, c)) return;
        if (board[r][c] == 0) return; // 빈 칸이면 무시

        ImageView targetStone = stoneViews[r][c];
        if (targetStone == null) {
            System.out.println("[GameBoard] SharedStone: 해당 위치에 ImageView가 없습니다. (r=" + r + ", c=" + c + ")");
            return;
        }

        try {
            Image sharedImg = new Image(
                    getClass().getResource("/images/cards/shared_stone.png").toExternalForm()
            );
            targetStone.setImage(sharedImg);
            sharedStones[r][c] = true;

            System.out.println("[GameBoard] SharedStone 적용 완료 at (" + r + ", " + c + ")");

            // 🔥 공용돌 포함 즉시 승리 여부 체크
            if (players != null) {
                if (isTeamMode2v2()) {
                    // 팀 기준으로 0팀 / 1팀 검사
                    for (int teamId = 0; teamId <= 1; teamId++) {
                        if (checkTeamWin(r, c, teamId)) {
                            // 대표 플레이어 하나 골라서 승자로 넘기기
                            int winnerIdx = 0;
                            for (int i = 0; i < playerTeam.length; i++) {
                                if (playerTeam[i] == teamId) {
                                    winnerIdx = i;
                                    break;
                                }
                            }
                            onGameOver(winnerIdx + 1);
                            return;
                        }
                    }
                } else {
                    // 기존 개인 기준
                    for (int sign = 1; sign <= players.length; sign++) {
                        if (checkWin(r, c, sign)) {
                            onGameOver(sign);
                            return;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 서버에서 "상대가 SharedStone 카드를 사용했다"는 이벤트를 받았을 때 호출.
     * - SharedStoneNotice.fxml 오버레이를 띄워 안내.
     */
    public void onSharedStoneStartFromOpponent() {
        System.out.println("[GameBoard] 상대가 SharedStone 카드를 사용했습니다.");

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/SharedStoneNotice.fxml") // ⚠️ 경로 확인 필요
            );
            StackPane overlay = loader.load();
            SharedStoneNoticeController controller = loader.getController();

            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 서버에서 "SharedStone 타겟 좌표"를 전달받았을 때 호출.
     * - 내 보드에도 동일한 공용돌 효과 적용.
     */
    public void onSharedStoneTargetFromOpponent(int r, int c) {
        System.out.println("[GameBoard] 서버로부터 SharedStone 타겟 좌표 수신: (" + r + ", " + c + ")");
        applySharedStoneAt(r, c);
        // 턴 전환은 서버의 TURN 메시지로 처리
    }

    // ================== 카드 슬롯 UI ==================
    /**
     * 카드 선택 화면에서 받은 카드 2장을 GameBoard에 표시하는 메서드.
     * - MatchSession에서 가져온 카드들을 UI 슬롯에 채운다.
     * - SHIELD 카드는 자동발동 카드라서 슬롯에는 표시하지 않는다.
     */
    public void setReceivedCards(List<Card> cards) {
        this.receivedCards = cards;

        hasShieldCard = false;
        cardSlotBox.getChildren().clear();

        if (cards == null || cards.isEmpty()) return;

        for (Card card : cards) {
            if (card == null) continue;

            switch (card.getType()) {
                case SHIELD -> {
                    hasShieldCard = true;

                    // 방어카드도 모서리에 살짝 표시 (자동발동이지만 '있다'는 건 보여주기)
                    ImageView iv = new ImageView(
                            new Image(getClass().getResource(card.getImagePath()).toExternalForm())
                    );
                    iv.setFitWidth(40);
                    iv.setFitHeight(40);
                    iv.setPreserveRatio(true);
                    iv.setOpacity(0.8); // 공격카드와 구분하고 싶으면 살짝 투명하게
                    iv.setStyle("-fx-effect: dropshadow(gaussian, black, 4, 0, 0, 0);");

                    cardSlotBox.getChildren().add(iv);
                }
                default -> {
                    ImageView iv = new ImageView(
                            new Image(getClass().getResource(card.getImagePath()).toExternalForm())
                    );
                    iv.setFitWidth(40);
                    iv.setFitHeight(40);
                    iv.setPreserveRatio(true);
                    iv.setStyle("-fx-effect: dropshadow(gaussian, black, 4, 0, 0, 0);");
                    cardSlotBox.getChildren().add(iv);
                }
            }
        }
    }

    // ================== 네트워크 바인딩 ==================
    public interface NetworkClient {
        void sendCheer(String msg);
        void sendPlace(int row, int col);

        void sendSharedStoneStart();
        void sendSharedStoneTarget(int row, int col);

        // Bomb!!
        void sendBombStart();
        void sendBombTarget(int row, int col);

        // Time Lock
        void sendTimeLockStart();

        // Swap
        void sendSwapStart();
        void sendSwapTarget(int myR, int myC, int oppR, int oppC);

        // DoubleMove
        void sendDoubleMoveStart();

        // Remove (상대 돌 제거)
        void sendRemoveStart();
        void sendRemoveTarget(int row, int col);

        // Shield (자동 방어) – 공격 카드 무효화 알림
        void sendShieldBlockForRemove();
        void sendShieldBlockForSwap();

        // 턴 종료 (서버가 턴을 넘기도록 요청)
        void sendTurnEnd();
    }

    private NetworkClient networkClient;

    /** GameIntroController에서 OmokClient와 연결해줄 때 호출 */
    public void bindNetwork(NetworkClient client) {
        this.networkClient = client;
        // 말풍선용 래핑 (기존 cheerSender 그대로 사용)
        this.cheerSender = client::sendCheer;
    }

    /**
     * 서버에서 "상대가 Time Lock 카드를 사용했다"는 알림을 받았을 때 호출.
     * - 내 다음 턴에 타이머를 3초로 세팅하기 위한 플래그 설정
     * - 하단 안내 오버레이(TimeLockNotice)를 띄움
     */
    public void onTimeLockStartFromOpponent() {
        System.out.println("[GameBoard] 상대가 Time Lock 카드를 사용했습니다.");

        // 내 "다음 턴"의 제한시간을 3초로 줄이는 플래그
        timeLockNextTurn = true;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/TimeLockNotice.fxml")
            );
            StackPane overlay = loader.load();
            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 턴 전환은 서버에서 TURN 메시지를 통해 관리
    }

    /** 2:2 팀전인지 여부 */
    private boolean isTeamMode2v2() {
        return players != null
                && players.length == 4
                && playerTeam != null
                && playerTeam.length == players.length;
    }

    // ================== Swap 카드 로직 ==================

    /** Swap 카드 사용 시작 (내가 카드 선택했을 때 호출) */
    private void useSwapCard() {
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 Swap 카드를 사용할 수 없습니다.");
            return;
        }

        System.out.println("[GameBoard] Swap 카드 사용!");

        // 서버에 Swap 사용 시작 알림
        if (networkClient != null) {
            networkClient.sendSwapStart();
        }

        enterSwapSelectMode();
    }

    /** Swap 선택 모드 진입: 안내 오버레이를 띄우고, 클릭은 handleSwapSelectClick에서 처리 */
    private void enterSwapSelectMode() {
        swapSelecting = true;
        swapMyPos = null;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/SwapSelectGuide.fxml")
            );
            StackPane overlay = loader.load();
            swapGuideController = loader.getController();
            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Swap 선택 모드에서 보드를 클릭했을 때: 1번 클릭은 내 돌, 2번 클릭은 상대 팀 돌 */
    private void handleSwapSelectClick(int r, int c) {
        if (!isInside(r, c)) return;

        // 1단계: 내 돌 선택
        if (swapMyPos == null) {
            if (board[r][c] != mySign) {
                System.out.println("[GameBoard] Swap: 내 돌이 아닌 곳을 클릭했습니다.");
                return;
            }
            swapMyPos = new int[]{r, c};
            if (swapGuideController != null) {
                swapGuideController.onMyStoneSelected();
            }
            System.out.println("[GameBoard] Swap: 내 돌 선택 (" + r + "," + c + ")");
            return;
        }

        // 2단계: 상대 팀 돌 선택
        int cellSign = board[r][c];

        if (cellSign == 0) {
            System.out.println("[GameBoard] Swap: 비어있는 칸을 클릭했습니다.");
            return;
        }

        // 팀 정보가 있는 경우: 같은 팀 돌은 교환 불가
        if (playerTeam != null && players != null
                && playerTeam.length == players.length) {

            int targetIdx = cellSign - 1;
            if (targetIdx < 0 || targetIdx >= playerTeam.length) {
                System.out.println("[GameBoard] Swap: 잘못된 sign 값 " + cellSign);
                return;
            }

            if (playerTeam[targetIdx] == playerTeam[myIndex]) {
                System.out.println("[GameBoard] Swap: 같은 팀 돌은 교환할 수 없습니다.");
                return;
            }
        } else {
            // 팀 정보가 없으면: 내 돌만 아니면 상대 돌 취급
            if (cellSign == mySign) {
                System.out.println("[GameBoard] Swap: 내 돌을 다시 선택했습니다.");
                return;
            }
        }

        int myR = swapMyPos[0];
        int myC = swapMyPos[1];
        int oppR = r;
        int oppC = c;

        System.out.println("[GameBoard] Swap: 상대 돌 선택 (" + oppR + "," + oppC + ")");

        swapSelecting = false;
        swapMyPos = null;

        if (swapGuideController != null) {
            swapGuideController.close();
            swapGuideController = null;
        }

        // 실제 교환 적용
        applySwapStones(myR, myC, oppR, oppC);

        // 서버에 좌표 전송 (상대 보드도 동일하게 변경)
        if (networkClient != null) {
            networkClient.sendSwapTarget(myR, myC, oppR, oppC);
        }

        // 교환 결과로 누가 이겼을 수도 있으므로 gameEnded 여부 확인
        if (!gameEnded) {
            endMyTurn();
        }
    }

    /** 두 좌표의 돌을 교환하고, 승리 여부를 검사한다. */
    private void applySwapStones(int myR, int myC, int oppR, int oppC) {
        if (!isInside(myR, myC) || !isInside(oppR, oppC)) return;

        // 보드 값(1 / -1) 교환
        int tmp = board[myR][myC];
        board[myR][myC] = board[oppR][oppC];
        board[oppR][oppC] = tmp;

        // 공용돌 플래그도 함께 교환 (혹시 나중에 공용돌과 섞여 쓸 수도 있으니까)
        boolean tmpShared = sharedStones[myR][myC];
        sharedStones[myR][myC] = sharedStones[oppR][oppC];
        sharedStones[oppR][oppC] = tmpShared;

        // 이미지 갱신
        refreshStoneImage(myR, myC);
        refreshStoneImage(oppR, oppC);

        System.out.println("[GameBoard] Swap 적용: (" + myR + "," + myC + ") <-> (" + oppR + "," + oppC + ")");

        // 교환 후 양쪽 모두 5목 체크
        checkWinAfterSwap(myR, myC, oppR, oppC);
    }

    /** 한 칸의 이미지를 현재 board / sharedStones 상태에 맞게 다시 그린다. */
    private void refreshStoneImage(int r, int c) {
        ImageView iv = stoneViews[r][c];
        if (iv == null) return;

        try {
            // 공용돌이면 무조건 공용돌 이미지
            if (sharedStones[r][c]) {
                Image sharedImg = new Image(
                        getClass().getResource("/images/cards/shared_stone.png").toExternalForm()
                );
                iv.setImage(sharedImg);
                return;
            }

            int sign = board[r][c]; // 1 ~ players.length
            if (sign <= 0 || players == null || stonePathOfPlayer == null) {
                return;
            }

            int idx = sign - 1;
            if (idx < 0 || idx >= stonePathOfPlayer.length) {
                System.out.println("[GameBoard] refreshStoneImage: 잘못된 sign=" + sign);
                return;
            }

            String path = stonePathOfPlayer[idx];
            if (path == null || path.isBlank()) {
                System.out.println("[GameBoard] refreshStoneImage: 이미지 경로 없음, sign=" + sign);
                return;
            }

            Image img = new Image(getClass().getResource(path).toExternalForm());
            iv.setImage(img);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Swap 후 승리 여부를 검사한다. */
    private void checkWinAfterSwap(int r1, int c1, int r2, int c2) {
        int[][] points = { {r1, c1}, {r2, c2} };

        for (int[] p : points) {
            int r = p[0];
            int c = p[1];
            int sign = board[r][c];   // 지금 이 칸에 놓여 있는 플레이어 sign (1..N)

            if (sign == 0) continue;  // 빈칸이면 스킵

            if (checkWin(r, c, sign)) {
                onGameOver(sign);
                return;
            }
        }
    }

    /**
     * 서버에서 "상대가 Swap 카드를 사용했다"는 알림을 받았을 때 호출.
     * - 중앙에 안내 오버레이(SwapNotice)를 띄움.
     */
    public void onSwapStartFromOpponent() {

        System.out.println("[GameBoard] 상대 Swap 사용됨");

        // 1순위: Defense로 자동 방어
        if (defenseReady) {
            handleDefenseAutoBlock("SWAP");
            return;
        }

        // 2순위: Shield 자동 방어
        if (hasShieldCard) {
            handleShieldDefenseFromAttack("SWAP");
            return;
        }

        // 방어 카드가 없으면, 그냥 안내 오버레이만 띄움
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/SwapNotice.fxml")
            );
            centerStack.getChildren().add(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 서버에서 "SWAP_TARGET myR myC oppR oppC"를 받았을 때 호출.
     * - 내 보드에도 동일한 위치 교환을 적용.
     */
    public void onSwapTargetFromOpponent(int myR, int myC, int oppR, int oppC) {
        System.out.println("[GameBoard] 서버로부터 Swap 타겟 좌표 수신: "
                + "(" + myR + "," + myC + ") <-> (" + oppR + "," + oppC + ")");

        // Shield로 이미 방어한 공격이면 교환 무시
        if (shieldBlockSwapPending) {
            System.out.println("[GameBoard] Shield로 인해 Swap 효과 무시");
            shieldBlockSwapPending = false;
            return;
        }

        applySwapStones(myR, myC, oppR, oppC);
        // 턴 전환은 서버 TURN 메시지로 처리
    }

    // ================== DoubleMove 카드 로직 ==================

    /**
     * DoubleMove 카드 사용 (내가 카드 선택했을 때 호출).
     * - 이번 턴에 내가 돌을 한 번 더 둘 수 있게 한다 (총 2번).
     * - 카드를 사용해도 턴을 바로 넘기지 않는다.
     */
    private void useDoubleMoveCard() {
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 DoubleMove 카드를 사용할 수 없습니다.");
            return;
        }

        System.out.println("[GameBoard] DoubleMove 카드 사용! 이 턴에 두 번 둘 수 있습니다.");

        // 현재 턴 플레이어에게 총 2수 부여
        movesLeftInCurrentTurn = 2;

        // 나도 화면 아래쪽에 배너 띄우기
        showDoubleMoveNotice("DOUBLE MOVE 사용! 이번 턴에 돌을 두 번 둘 수 있습니다.");

        // 서버에 알림 (상대 화면에서도 안내 배너 + 동일한 movesLeft 설정)
        if (networkClient != null) {
            networkClient.sendDoubleMoveStart();
        }

        // DoubleMove는 "돌 두기 강화"이기 때문에 턴은 여기서 종료하지 않는다.
    }

    /**
     * 서버에서 "상대가 DoubleMove 카드를 사용했다"는 알림을 받았을 때 호출.
     * - 이번 턴의 플레이어(상대)에게 총 2수 부여.
     * - 하단 안내 배너(DoubleMoveNotice)를 띄운다.
     */
    public void onDoubleMoveStartFromOpponent() {
        System.out.println("[GameBoard] 상대가 DoubleMove 카드를 사용했습니다.");

        // 현재 턴은 상대이지만, 이 턴 전체가 2수로 확장되므로
        movesLeftInCurrentTurn = 2;

        // 내 화면에도 안내 배너를 띄우기
        showDoubleMoveNotice("상대가 DOUBLE MOVE 카드를 사용했습니다.\n이번 턴에 상대가 돌을 두 번 둡니다.");
    }

    /** DoubleMove용 안내 배너를 화면 아래쪽에 띄우는 공통 메서드 */
    private void showDoubleMoveNotice(String message) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/DoubleMoveNotice.fxml")
            );
            StackPane overlay = loader.load();

            DoubleMoveNoticeController controller = loader.getController();
            controller.setMessage(message);

            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ================== Remove 카드 로직 (상대 돌 1개 제거) ==================

    /** Remove 카드 사용 시작 (내가 카드 선택했을 때 호출) */
    private void useRemoveCard() {
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 Remove 카드를 사용할 수 없습니다.");
            return;
        }

        System.out.println("[GameBoard] Remove 카드 사용!");

        // 서버에 Remove 사용 시작 알림
        if (networkClient != null) {
            networkClient.sendRemoveStart();
        }

        enterRemoveSelectMode();
    }

    /** Remove 선택 모드 진입: 안내 오버레이 + 상대 돌 선택 대기 */
    private void enterRemoveSelectMode() {
        removeSelecting = true;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/RemoveGuide.fxml")
            );
            StackPane overlay = loader.load();
            removeGuideController = loader.getController();

            // ✅ 오버레이는 화면에 보이기만 하고, 마우스는 아래(boardRoot)로 통과시키기
            overlay.setMouseTransparent(true);

            // 콜백 등록 (현재 구조에서는 boardRoot 클릭 → GameBoardController가 notifyStoneSelected 호출)
            removeGuideController.setOnStoneSelected((row, col) -> {
                onRemoveTargetChosenByMe(row, col);
            });

            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Remove 선택 모드에서 보드를 클릭했을 때 */
    private void handleRemoveTargetClick(int r, int c) {
        if (!isInside(r, c)) return;

        int cellSign = board[r][c];

        // 빈 칸 + 공용 아님 → 선택 불가
        if (cellSign == 0 && !sharedStones[r][c]) {
            System.out.println("[GameBoard] Remove: 빈 칸을 클릭했습니다.");
            return;
        }

        // 공용돌(sharedStones)은 항상 제거 가능
        if (!sharedStones[r][c]) {
            // 팀 정보가 있는 경우: 같은 팀(나/팀원) 돌은 제거 불가
            if (playerTeam != null && players != null
                    && playerTeam.length == players.length) {

                int targetIdx = cellSign - 1;
                if (targetIdx < 0 || targetIdx >= playerTeam.length) {
                    System.out.println("[GameBoard] Remove: 잘못된 sign 값 " + cellSign);
                    return;
                }

                if (playerTeam[targetIdx] == playerTeam[myIndex]) {
                    System.out.println("[GameBoard] Remove: 같은 팀 돌은 제거할 수 없습니다.");
                    return;
                }
            } else {
                // 팀 정보가 없으면: 내 돌만 아니면 제거 가능
                if (cellSign == mySign) {
                    System.out.println("[GameBoard] Remove: 자신의 돌은 제거할 수 없습니다.");
                    return;
                }
            }
        }

        if (removeGuideController != null) {
            removeGuideController.notifyStoneSelected(r, c);
        } else {
            onRemoveTargetChosenByMe(r, c);
        }
    }

    private void onRemoveTargetChosenByMe(int r, int c) {
        removeSelecting = false;

        // 안내 오버레이 닫기
        if (removeGuideController != null) {
            removeGuideController.close();
            removeGuideController = null;
        }

        applyRemoveAt(r, c);

        // ✅ 내가 쓴 Remove 카드 안내 (내 화면 아래쪽 배너)
        showRemoveNotice(
                "Remove",
                "상대방의 돌 1개를 제거했습니다."
        );

        if (networkClient != null) {
            networkClient.sendRemoveTarget(r, c);
        }

        if (!gameEnded) {
            endMyTurn();
        }
    }

    /** (r,c)의 돌을 제거한다. */
    private void applyRemoveAt(int r, int c) {
        if (!isInside(r, c)) return;
        if (board[r][c] == 0) return;

        ImageView stone = stoneViews[r][c];
        if (stone != null) {
            boardRoot.getChildren().remove(stone);
        }

        board[r][c] = 0;
        stoneViews[r][c] = null;
        sharedStones[r][c] = false;

        System.out.println("[GameBoard] Remove 적용: (" + r + ", " + c + ")의 돌 제거");
    }

    /** 서버에서 '상대가 Remove 카드를 사용했다' 알림을 받았을 때 */
    public void onRemoveStartFromOpponent() {

        System.out.println("[GameBoard] 상대 Remove 사용됨");

        // 1순위: Defense 자동 방어
        if (defenseReady) {
            handleDefenseAutoBlock("REMOVE");
            return;
        }

        // 2순위: Shield 자동 방어
        if (hasShieldCard) {
            handleShieldDefenseFromAttack("REMOVE");
            return;
        }

        // 방어 카드 없으면 기존처럼 안내
        showRemoveNotice("Remove", "상대가 공격카드를 사용했습니다.\n당신의 돌이 제거됩니다.");
    }

    /** 서버에서 'REMOVE_TARGET r c' 를 받았을 때 */
    public void onRemoveTargetFromOpponent(int r, int c) {
        System.out.println("[GameBoard] Remove 타겟 좌표 수신: (" + r + ", " + c + ")");

        // Shield로 이미 방어한 공격이면 실제 제거 무시
        if (shieldBlockRemovePending) {
            System.out.println("[GameBoard] Shield로 인해 Remove 효과 무시");
            shieldBlockRemovePending = false;
            return;
        }

        applyRemoveAt(r, c);
        // 턴 전환은 서버 TURN 메시지로 처리
    }

    // ================== Remove 안내 배너 공통 메서드 ==================
    private void showRemoveNotice(String title, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/RemoveNotice.fxml")
            );
            StackPane overlay = loader.load();
            RemoveNoticeController controller = loader.getController();
            if (title != null) controller.setTitle(title);
            if (message != null) controller.setMessage(message);

            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** SHIELD 를 제외한 실제 선택 가능한 카드 리스트 */
    private List<Card> getUsableCardsForModal() {
        if (receivedCards == null || receivedCards.isEmpty()) {
            return Collections.emptyList();
        }
        List<Card> usable = new ArrayList<>();
        for (Card card : receivedCards) {
            if (card == null) continue;
            switch (card.getType()) {
                case SHIELD -> { /* 자동 발동이라 모달에서는 제외 */ }
                default -> usable.add(card);
            }
        }
        return usable;
    }

    /** SHIELD 카드 1장을 소모하고 슬롯 UI를 다시 그린다. */
    private void consumeShieldCard() {
        hasShieldCard = false;
        if (receivedCards == null || receivedCards.isEmpty()) return;

        receivedCards.removeIf(card -> {
            if (card == null) return false;
            return switch (card.getType()) {
                case SHIELD -> true;
                default -> false;
            };
        });

        // Shield는 슬롯에 안 보이지만, 남은 카드 슬롯을 다시 갱신
        setReceivedCards(receivedCards);
    }

    /** 방어 측(피격자)용 안내 팝업 */
    private void showShieldNoticeForDefender() {
        showShieldNotice(
                "Shield",
                "상대방이 공격카드를 사용했습니다\n카드가 자동발동하여 방어에 성공했습니다"
        );
    }

    /** 공격 측(카드 사용자)용 안내 팝업 */
    private void showShieldNoticeForAttacker() {
        showShieldNotice(
                "Shield",
                "상대방이 방어카드를 사용했습니다\n당신의 공격은 실패했습니다"
        );
    }

    private void showShieldNotice(String title, String message) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/ShieldNotice.fxml")
            );
            StackPane overlay = loader.load();
            ShieldNoticeController controller = loader.getController();
            controller.setTexts(title, message);
            centerStack.getChildren().add(overlay);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove / Swap 공격카드가 들어왔을 때 Shield 로 막는 공통 처리
     * @param attackType "REMOVE" 또는 "SWAP"
     */
    private void handleShieldDefenseFromAttack(String attackType) {
        if (!hasShieldCard) return;

        System.out.println("[GameBoard] Shield 카드 자동 발동! attackType = " + attackType);

        // 1) 내 쪽(피격자) 방어 연출 + 카드 소모
        consumeShieldCard();
        showShieldNoticeForDefender();

        // 2) 공격자에게 "막혔다" 알림 전송 + 이쪽에서는 이후 타겟을 무시하기 위한 플래그 세팅
        if (networkClient != null) {
            switch (attackType) {
                case "REMOVE" -> {
                    shieldBlockRemovePending = true;
                    networkClient.sendShieldBlockForRemove();
                }
                case "SWAP" -> {
                    shieldBlockSwapPending = true;
                    networkClient.sendShieldBlockForSwap();
                }
            }
        }

        // 3) 턴 관리는 서버가 처리하므로 여기서는 턴 종료/전환 로직을 넣지 않는다.
    }

    /** 서버에서 'SHIELD_BLOCK_REMOVE' 수신: 내가 쓴 Remove 가 상대 Shield 에 막힘 */
    public void onShieldBlockRemoveFromOpponent() {
        System.out.println("[GameBoard] 내 Remove 카드가 상대의 Shield/Defense에 의해 막혔습니다.");

        // Remove 선택 모드/가이드 종료
        removeSelecting = false;
        if (removeGuideController != null) {
            removeGuideController.close();
            removeGuideController = null;
        }

        showShieldNoticeForAttacker();

        // 🔥 여기 추가: 아직도 내 턴이면(=Defense로 막힌 경우) 턴을 종료해 준다.
        if (!gameEnded && isMyTurn()) {
            endMyTurn();
        }
    }

    /** 서버에서 'SHIELD_BLOCK_SWAP' 수신: 내가 쓴 Swap 이 상대 Shield 에 막힘 */
    public void onShieldBlockSwapFromOpponent() {
        System.out.println("[GameBoard] 내 Swap 카드가 상대의 Shield/Defense에 의해 막혔습니다.");

        swapSelecting = false;
        swapMyPos = null;
        if (swapGuideController != null) {
            swapGuideController.close();
            swapGuideController = null;
        }

        showShieldNoticeForAttacker();

        // 🔥 여기 추가: 아직도 내 턴이면(=Defense로 막힌 경우) 턴을 종료해 준다.
        if (!gameEnded && isMyTurn()) {
            endMyTurn();
        }
    }

    /**
     * DEFENSE 카드 사용 (내가 사용)
     * - 턴이 유지되고 돌도 둘 수 있다.
     * - 상대는 내가 사용했는지 모른다.
     * - 이번 상대 턴의 REMOVE / SWAP 1회 자동 방어.
     */
    private void useDefenseCard() {
        if (!isMyTurn()) {
            System.out.println("[GameBoard] 내 턴이 아니라 Defense 카드를 사용할 수 없습니다.");
            return;
        }

        System.out.println("[GameBoard] Defense 카드 사용! 다음 상대 공격(Remove/Swap) 1회 자동 방어.");

        // 이번 상대 턴 동안 유효한 방어 버프
        defenseReady = true;

        // 안내 배너 띄우기 (내 화면에만)
        showDefenseActivatedNotice();
    }

    /** 하단 안내 배너: Defense 사용 직후 */
    private void showDefenseActivatedNotice() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/DefenseNotice.fxml")
            );
            StackPane overlay = loader.load();

            DefenseNoticeController controller = loader.getController();
            controller.setTexts(
                    "Defense",
                    "Defense 방어카드를 사용했습니다.\n다음 상대 턴의 Remove/Swap 공격을 자동으로 방어합니다."
            );

            centerStack.getChildren().add(overlay);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Defense 자동 방어 처리
     * @param attackType "REMOVE" 또는 "SWAP"
     */
    private void handleDefenseAutoBlock(String attackType) {
        if (!defenseReady) return;

        System.out.println("[GameBoard] Defense 자동 발동! attackType = " + attackType);

        // 이번 Defense 버프는 한 번만 유효
        defenseReady = false;

        // 이후 들어오는 타겟 좌표(Remove/Swap)는 무시하기 위해 플래그 설정
        if ("REMOVE".equals(attackType)) {
            shieldBlockRemovePending = true;
            if (networkClient != null) {
                networkClient.sendShieldBlockForRemove();   // 공격자에게 '막혔다' 알림
            }
        } else if ("SWAP".equals(attackType)) {
            shieldBlockSwapPending = true;
            if (networkClient != null) {
                networkClient.sendShieldBlockForSwap();
            }
        }

        // 내 화면에 Defense 방어 성공 안내 (ShieldNotice UI 재활용)
        showDefenseNoticeForDefender();
    }

    /** 방어 측(나)용 Defense 방어 성공 안내 */
    private void showDefenseNoticeForDefender() {
        // ShieldNotice.fxml + ShieldNoticeController를 재활용해서 텍스트만 바꾸자
        showShieldNotice(
                "Defense",
                "상대의 공격카드를 미리 사용한 Defense로 방어했습니다."
        );
    }

}