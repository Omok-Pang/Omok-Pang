package com.omokpang.controller.game;

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
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Duration;

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

    // 말풍선 버튼 (왼쪽 아래)
    @FXML private Button messageButton;

    // 왼쪽 말풍선 선택 패널
    @FXML private StackPane messageSelectPane;   // 전체 패널
    @FXML private VBox messageListBox;          // 패널 안의 메시지 목록 컨테이너

    // 아래 유저 말풍선 영역 (말풍선 이미지 + 텍스트)
    @FXML private StackPane bottomMessageBubble;
    @FXML private Label bottomMessageLabel;

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

    private Timeline timer;   // 1초마다 동작하는 타이머
    private int remain = 20;  // 남은 시간(초)

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

    /* ================== 초기화 ================== */

    @FXML
    public void initialize() {
        // 아바타 컨테이너가 가로로 쭉 늘어지지 않도록
        bottomPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
        bottomPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);

        // 필요 시 위쪽 유저도 동일하게 처리
        topPlayerContainer.setMaxWidth(Region.USE_PREF_SIZE);
        topPlayerContainer.setMaxHeight(Region.USE_PREF_SIZE);

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
        remain = 20;
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
}
