package com.omokpang.controller.game;

import com.omokpang.controller.effect.TimeLockNoticeController;
import com.omokpang.controller.effect.SwapSelectGuideController;
import com.omokpang.controller.effect.SwapNoticeController;

import javafx.fxml.FXMLLoader;
import java.io.IOException;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import com.omokpang.domain.card.Card;
import java.util.List;

/**
 * 역할:
 *  - 실제 오목판을 그리고, 유저가 돌을 두는 로직을 관리한다.
 *  - 위/아래 유저(프로필 이미지)와 현재 턴 표시를 관리한다.
 *  - 제한 시간(20초) 타이머를 관리한다.
 *  - 말풍선 선택/표시 플로우를 관리한다.
 *
 * 추후 확장 방향:
 *  - 온라인 대전일 경우, 현재 보드 상태/턴 정보/메시지를 서버와 동기화해야 한다.
 *  - WebSocket 등을 사용해 상대방에게 말풍선/돌 두기 이벤트를 실시간으로 보내도록 수정.
 */
public class GameBoardController {

    // 루트 레이아웃
    @FXML private BorderPane rootPane;

    // ✅ center 영역 최상단 StackPane (오버레이를 얹을 컨테이너)
    @FXML private StackPane centerStack;

    // 보드 UI (360x360 Pane)
    @FXML private Pane boardRoot;

    // 상단 타이머 / 턴 안내
    @FXML private Label timerLabel;
    @FXML private Label turnLabel;

    // 위/아래 플레이어 아바타 컨테이너 및 이미지
    @FXML private StackPane topPlayerContainer;
    @FXML private StackPane bottomPlayerContainer;

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

    // 아래 유저 말풍선 영역 (말풍선 이미지 + 텍스트)
    @FXML private StackPane bottomMessageBubble;
    @FXML private Label bottomMessageLabel;

    // 🔹 선택된 카드 아이콘 표시 영역 (오른쪽 아래)
    @FXML private HBox cardSlotBox;

    // 카드 선택 화면에서 전달받은 카드 두 장
    private List<Card> receivedCards;

    /* ================== 보드 / 턴 관련 상수 & 상태 ================== */

    // 보드 크기 (15 x 15)
    private static final int N = 15;

    // 보드 한 변 길이(px)
    private static final double SIZE = 360;

    // 한 칸(격자 간격) 크기
    private static final double CELL = SIZE / (N - 1);

    // 보드 상태: 0=빈칸, 1=위 유저의 돌, -1=아래 유저의 돌
    private final int[][] board = new int[N][N];

    // 현재 턴(누가 둘 차례인지): 1=위 유저, -1=아래 유저
    private int current = 1;

    /* ================== 타이머 관련 ================== */

    private static final int DEFAULT_TURN_SECONDS = 20; // 기본 턴 시간
    private static final int TIMELOCK_TURN_SECONDS = 3; // Time Lock 적용 시 턴 시간

    private Timeline timer;   // 1초마다 동작하는 타이머
    private int remain = DEFAULT_TURN_SECONDS;  // 남은 시간(초)

    /* ================== 프리셋 말풍선 텍스트 ================== */

    // 왼쪽 패널에 표시되는 15개의 응원 메시지
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

    /* ================== Swap 카드 관련 상태 ================== */

    // Swap 선택 안내 오버레이 컨트롤러 (내 화면용)
    private SwapSelectGuideController swapGuideController;
    // 추후 실제 돌 선택 모드 분기용 플래그 (지금은 안내만 띄우는 용도)
    private boolean swapSelecting = false;

    /* ================== 초기화 ================== */

    @FXML
    public void initialize() {
        // 아바타 컨테이너가 가로로 쭉 늘어지지 않도록
        bottomPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
        bottomPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);

        // 필요 시 위쪽 유저도 동일하게 처리
        topPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
        topPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);

        // 좌/우 플레이어도 동일하게 (null 체크는 방어용)
        if (leftPlayerContainer != null) {
            leftPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
            leftPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);
        }
        if (rightPlayerContainer != null) {
            rightPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
            rightPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);
        }

        // 보드 사이즈(360x360) 설정 및 격자 그리기
        boardRoot.setPrefSize(SIZE, SIZE);
        drawGrid();

        // 보드 클릭 시 돌 놓기
        boardRoot.setOnMouseClicked(e -> {
            int c = (int) Math.round(e.getX() / CELL);
            int r = (int) Math.round(e.getY() / CELL);
            place(r, c);
        });

        // 말풍선 패널 / 말풍선 버블은 초기에는 숨겨둔다
        messageSelectPane.setVisible(false);
        bottomMessageBubble.setVisible(false);

        // 왼쪽 말풍선 선택 리스트 구성
        setupMessageList();

        // 초기 턴 표시 및 아바타 하이라이트
        updateTurnLabel();
        updateActivePlayerHighlight();

        // 턴 타이머 시작
        startTurn();

        // TODO: 서버와 연결 시, 초기 보드 상태/현재 턴 정보를 서버에서 받아서 여기서 동기화
    }

    /* ================== 말풍선 리스트 UI 구성 ================== */

    /**
     * 좌측 말풍선 선택 패널에 PRESET_MESSAGES 배열을 이용해 항목을 생성한다.
     */
    private void setupMessageList() {
        messageListBox.getChildren().clear();

        for (String text : PRESET_MESSAGES) {
            Region item = createMessageItem(text);
            messageListBox.getChildren().add(item);
        }
    }

    /**
     * 개별 말풍선 항목 UI를 만든다.
     *  - 배경 이미지(ui_select.png) + Label 텍스트
     *  - 클릭 시 sendBalloon(text) 호출
     */
    private Region createMessageItem(String text) {
        // 파란 바 이미지
        Image bgImg = new Image(
                getClass().getResource("/images/message/ui_select.png").toExternalForm()
        );
        ImageView bgView = new ImageView(bgImg);
        bgView.setPreserveRatio(true);
        bgView.setFitWidth(200); // 항목 폭

        // 텍스트 라벨
        Label label = new Label(text);
        label.setStyle(
                "-fx-text-fill: #000000;" + "-fx-font-size: 14;" + "-fx-font-weight: 700;"
        );

        // 이미지 + 텍스트를 겹쳐서 배치
        StackPane item = new StackPane(bgView, label);
        item.setPrefWidth(200);
        item.setMaxWidth(200);

        // 클릭 시 내 말풍선으로 전송
        item.setOnMouseClicked(e -> sendBalloon(text));

        // 항목 위/아래 여백
        VBox.setMargin(item, new Insets(2, 0, 2, 0));

        return item;
    }

    /* ================== 보드 그리기 및 돌 놓기 ================== */

    /**
     * 보드 Pane 위에 15x15 격자와 외곽선을 그린다.
     */
    private void drawGrid() {
        boardRoot.getChildren().clear();

        // 내부 격자선(가로/세로)
        for (int i = 0; i < N; i++) {
            double p = i * CELL;

            Line h = new Line(0, p, SIZE, p);
            Line v = new Line(p, 0, p, SIZE);

            h.setStroke(Color.color(1, 1, 1, 0.25));
            v.setStroke(Color.color(1, 1, 1, 0.25));
            boardRoot.getChildren().addAll(h, v);
        }

        // 외곽선 4개
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

    /**
     * (r, c) 위치에 현재 플레이어의 돌을 놓는다.
     *  - 클릭한 좌표를 CELL 단위로 나눈 뒤 반올림해서 인덱스를 계산한다.
     *  - 보드 범위 밖이거나 이미 돌이 있으면 무시.
     */
    private void place(int r, int c) {
        if (!isInside(r, c) || board[r][c] != 0) return;

        // 격자 교차점의 실제 좌표(px)
        double cx = c * CELL;
        double cy = r * CELL;

        // 유저별 돌 이미지 경로
        String stonePath = (current == 1)
                ? "/images/user/sm_user1.png"
                : "/images/user/sm_user2.png";

        Image img = new Image(getClass().getResource(stonePath).toExternalForm());
        ImageView stone = new ImageView(img);

        double stoneSize = CELL * 0.9;
        stone.setFitWidth(stoneSize);
        stone.setFitHeight(stoneSize);
        stone.setPreserveRatio(true);

        // 교차점 중심에 맞게 위치 보정
        stone.setLayoutX(cx - stoneSize / 2);
        stone.setLayoutY(cy - stoneSize / 2);

        // Pane에 추가 + 보드 상태 갱신
        boardRoot.getChildren().add(stone);
        board[r][c] = current;

        // TODO: 여기에서 승리 조건(5목 완성 여부) 검사 로직 추가 가능
        // TODO: 온라인 모드일 경우, 이 돌 두기를 서버에 전송해서 상대에게도 반영해야 함.

        // 턴 전환
        current *= -1;
        updateTurnLabel();
        updateActivePlayerHighlight();
        restartTimer();
    }

    /** 보드 인덱스(r,c)가 유효한 범위인지 체크 */
    private boolean isInside(int r, int c) {
        return r >= 0 && r < N && c >= 0 && c < N;
    }

    /* ================== 아바타 하이라이트 / 턴 텍스트 ================== */

    /**
     * 현재 턴인 플레이어의 아바타에 빨간 테두리를 그려준다.
     */
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

        if (current == 1) {
            topPlayerContainer.setStyle(activeStyle);
            bottomPlayerContainer.setStyle(inactiveStyle);
        } else {
            topPlayerContainer.setStyle(inactiveStyle);
            bottomPlayerContainer.setStyle(activeStyle);
        }
    }

    /**
     * 하단의 "내 턴 / 상대 턴" 텍스트를 갱신한다.
     */
    private void updateTurnLabel() {
        if (current == 1) {
            turnLabel.setText("상대 턴 (위 유저)");
        } else {
            turnLabel.setText("내 턴 (아래 유저)");
        }
    }

    /* ================== 턴 타이머 로직 ================== */

    /**
     * 새 턴이 시작될 때 타이머를 초기화하고 20초 카운트다운을 시작한다.
     */
    private void startTurn() {
        startTurnWithSeconds(DEFAULT_TURN_SECONDS);
    }

    /** seconds 만큼의 제한시간으로 턴 타이머 시작 (TimeLock에서도 재사용) */
    private void startTurnWithSeconds(int seconds) {
        stopTimer(); // 기존 타이머 정지

        remain = seconds;
        timerLabel.setText(remain + "초");

        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remain--;
            timerLabel.setText(remain + "초");

            if (remain <= 0) {
                // 시간 초과 → 그냥 턴만 넘기기 (돌은 두지 않음)
                current *= -1;
                updateTurnLabel();
                updateActivePlayerHighlight();
                restartTimer();

                // TODO: 서버 연동 시, 타임아웃 발생 이벤트를 서버에 알려서 상대 클라이언트에서도 턴이 넘어가도록 처리해야 함.
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.playFromStart();
    }

    /** 현재 타이머를 멈추고 새로 시작 */
    private void restartTimer() {
        stopTimer();
        startTurn();
    }

    /** 타이머 정지 */
    private void stopTimer() {
        if (timer != null) timer.stop();
    }

    /* ================== 말풍선 플로우 ================== */

    /**
     * 하단 말풍선 버튼 클릭 시 호출.
     *  - 왼쪽 말풍선 선택 패널을 토글(보이기/숨기기) 한다.
     */
    @FXML
    public void handleCheer() {
        boolean nowVisible = messageSelectPane.isVisible();
        messageSelectPane.setVisible(!nowVisible);
    }

    /**
     * 왼쪽 패널에서 메시지를 하나 선택했을 때 호출되는 로직.
     *  - 선택 패널을 닫고
     *  - 아래 유저 아바타 옆에 말풍선을 2초 동안 보여준다.
     */
    private void sendBalloon(String text) {
        // 선택 패널 닫기
        messageSelectPane.setVisible(false);

        // 말풍선 텍스트 갱신 + 보이기
        bottomMessageLabel.setText(text);
        bottomMessageBubble.setVisible(true);

        // 2초 뒤 자동으로 말풍선 숨기기
        PauseTransition hide = new PauseTransition(Duration.seconds(2));
        hide.setOnFinished(e -> bottomMessageBubble.setVisible(false));
        hide.play();

        // TODO: 온라인 모드일 경우
        //  - 이 메시지를 서버로 보내서 상대 화면에도 같은 말풍선이 뜨도록 해야 한다.
        //  - 예: websocket.send({type:"CHEER", message:text})

    }

    /* ================== 카드 선택 모달 ================== */

    /**
     * 오른쪽 아래 카드 버튼 클릭 시 호출.
     *  - CardUseModal.fxml을 로드해서 centerStack 위에 오버레이로 올린다.
     *  - 현재 보유중인 카드(receivedCards)를 모달에 전달.
     */
    // ❌ @FXML 제거 + 이름만 살짝 변경 (선택사항, 유지하고 싶으면 그대로 두고 호출만 바꿔도 됨)
    private void openCardUseModal() {
        // 아직 받은 카드가 없으면 모달을 열지 않음
        if (receivedCards == null || receivedCards.isEmpty()) {
            System.out.println("[DEBUG] 사용 가능한 카드가 없습니다.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/game/CardUseModal.fxml")
            );
            StackPane modalRoot = loader.load();
            CardUseModalController modalController = loader.getController();

            // 🔹 현재 보유 카드 전달 → la_이미지로 렌더링됨
            modalController.setCards(receivedCards);

            // 🔹 선택된 카드에 대한 콜백 (지금은 콘솔 출력만)
            modalController.setOnCardSelected(selectedCard -> {
                System.out.println("[DEBUG] 선택된 카드: " + selectedCard.getName());
                // TODO: 이후에 카드 타입에 따라 효과 분기
                // switch (selectedCard.getType()) { ... }
            });

            centerStack.getChildren().add(modalRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ================== Time Lock 카드 효과 ================== */

    /**
     * Time Lock 카드가 상대에게 적용될 때 호출되는 메서드.
     *  - 현재 타이머를 멈추고
     *  - TimeLockNotice.fxml 오버레이를 2초간 보여준 뒤
     *  - 제한시간을 3초로 줄여서 다시 타이머 시작
     */
    private void applyTimeLockToOpponent() {
        stopTimer(); // 기존 20초 타이머 정지

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/TimeLockNotice.fxml")
            );
            loader.load();
            TimeLockNoticeController noticeController = loader.getController();

            // 안내창을 centerStack 위에 올리고, 2초 뒤 사라지면 3초 타이머 시작
            noticeController.showOn(centerStack, () -> {
                startTurnWithSeconds(TIMELOCK_TURN_SECONDS);
            });
        } catch (IOException e) {
            e.printStackTrace();
            // 안내창 로드에 실패해도 타임락 효과(3초 타이머)는 적용되도록
            startTurnWithSeconds(TIMELOCK_TURN_SECONDS);
        }
    }

    /* ================== Swap 카드 효과 ================== */

    /**
     * Swap 카드를 사용한 "나의 화면"에서
     * 돌 선택 안내 오버레이를 띄우는 메서드.
     *  (실제 돌 선택/교환 로직은 추후 swapSelecting 플래그를 기준으로 구현)
     */
    private void enterSwapSelectionMode() {
        swapSelecting = true;   // 추후 보드 클릭 로직에서 이 값으로 분기 예정

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/SwapSelectGuide.fxml")
            );
            loader.load();
            swapGuideController = loader.getController();
            // centerStack 맨 위에 안내 오버레이 올리기
            swapGuideController.showOn(centerStack);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * (추후 사용) Swap 선택/교환이 모두 끝났을 때
     * 내 화면에서 안내 오버레이를 닫고 선택 모드를 해제한다.
     *  - 나중에 "Enter 눌렀을 때" 실제 교환까지 끝난 시점에 호출 예정.
     */
    private void finishSwapSelectionMode() {
        swapSelecting = false;
        if (swapGuideController != null) {
            swapGuideController.close();
            swapGuideController = null;
        }
    }

    /**
     * (온라인 전용) "상대방이 Swap 카드를 썼다"는 이벤트가
     * 네 클라이언트로 들어왔을 때 호출하면 되는 메서드.
     *  - TimeLockNotice 와 동일하게 2초 안내만 보여주고 닫힌다.
     */
    public void showSwapUsedByOpponent() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/effect/SwapNotice.fxml")
            );
            loader.load();
            SwapNoticeController controller = loader.getController();
            controller.showOn(centerStack, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 카드 선택 화면에서 전달받은 카드 리스트 세팅
     *  - me_*.png 경로를 받아와서
     *    보드 화면에서는 동일 이름의 sm_*.png 아이콘으로 표시한다.
     */
    public void setReceivedCards(List<Card> cards) {
        this.receivedCards = cards;
        renderCardSlots();
    }

    /** 오른쪽 아래 cardSlotBox 에 sm_* 아이콘들을 렌더링 */
    private void renderCardSlots() {
        if (cardSlotBox == null) return;

        cardSlotBox.getChildren().clear();
        if (receivedCards == null || receivedCards.isEmpty()) {
            return;
        }

        for (Card card : receivedCards) {
            String bigPath = card.getImagePath();           // 예: /images/gamecard/me_Defense.png
            String smallPath = toSmallImagePath(bigPath);   // 예: /images/gamecard/sm_Defense.png

            Image img = new Image(
                    getClass().getResource(smallPath).toExternalForm()
            );
            ImageView iv = new ImageView(img);
            iv.setFitHeight(60);
            iv.setPreserveRatio(true);

            // 👉 아이콘 자체를 버튼으로 만들어서 눌렀을 때 모달 오픈
            Button btn = new Button();
            btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            btn.setGraphic(iv);

            // 지금은 어떤 카드를 눌러도 동일하게 "보유 카드 선택 모달"을 띄우도록 처리
            btn.setOnAction(e -> openCardUseModal());

            cardSlotBox.getChildren().add(btn);
        }
    }

    /**
     * me_*.png 경로를 sm_*.png 경로로 변환한다.
     *  - 카드 이미지 파일 구조:
     *      /images/gamecard/me_카드이름.png  (카드 선택 화면)
     *      /images/gamecard/sm_카드이름.png  (게임 보드 오른쪽 아이콘)
     */
    private String toSmallImagePath(String bigPath) {
        if (bigPath == null) return "/images/gamecard/sm_SharedStone.png";
        // 안전하게 gamecard 디렉터리 기준으로만 치환
        return bigPath.replace("/me_", "/sm_");
    }
}