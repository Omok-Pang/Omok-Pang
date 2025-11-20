/** MainController
 * 역할: 홈(개인전/팀전, 2·4인 선택, 규칙보기).
 * 핵심기능: 모드/인원 선택 상태 유지 / 규칙 모달 / 게임 시작→매칭 화면 이동.
 */
package com.omokpang.controller.main;

import com.omokpang.SceneRouter;
import com.omokpang.domain.user.User;
import com.omokpang.session.AppSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;

public class MainController {

    // 상단 사용자 정보
    @FXML private ImageView imgProfile;
    @FXML private Label labelUsername;
    @FXML private Label labelPoint;
    @FXML private Label labelWin;

    // 선택 이미지들
    @FXML private ImageView imgSolo;
    @FXML private ImageView imgTeam;
    @FXML private ImageView imgSize2;
    @FXML private ImageView imgSize4;

    // 선택 상태
    private boolean isTeamMode = false;
    private int playerCount = 0;

    private static final String SOLO_GRAY   = "/images/main/single_gray.png";
    private static final String SOLO_COLOR  = "/images/main/single_color.png";
    private static final String TEAM_GRAY   = "/images/main/team_gray.png";
    private static final String TEAM_COLOR  = "/images/main/team_color.png";
    private static final String SIZE2_GRAY  = "/images/main/2p_gray.png";
    private static final String SIZE2_COLOR = "/images/main/2p_color.png";
    private static final String SIZE4_GRAY  = "/images/main/4p_gray.png";
    private static final String SIZE4_COLOR = "/images/main/4p_color.png";

    @FXML
    private void initialize() {
        // 1) 버튼 초기화
        resetModeImages();
        resetSizeImages();

        // 2) 로그인 세션에서 유저 정보 읽어서 상단 바에 세팅
        User user = AppSession.getCurrentUser();
        if (user != null) {
            labelUsername.setText(user.getNickname());        // 닉네임
            labelPoint.setText(String.valueOf(user.getPoints())); // 포인트
            labelWin.setText(String.valueOf(user.getWins()));     // 승리 수

            // 프로필 이미지를 User에 따로 저장하고 있다면 여기서 세팅
            // 예: user.getProfileImagePath()
            // String profilePath = user.getProfileImagePath();
            // if (profilePath != null && !profilePath.isBlank()) {
            //     Image profileImg = loadImg(profilePath);
            //     if (profileImg != null) imgProfile.setImage(profileImg);
            // }
        }
    }

    /* ==================== 모드 선택 (개인전 / 팀전) ==================== */

    /** 개인전 버튼 클릭 */
    @FXML
    private void handleSelectSolo() {
        isTeamMode = false;
        resetModeImages();
        imgSolo.setImage(loadImg(SOLO_COLOR));
    }

    /** 팀전 버튼 클릭 */
    @FXML
    private void handleSelectTeam() {
        isTeamMode = true;
        resetModeImages();
        resetSizeImages();
        playerCount = 0;
        imgTeam.setImage(loadImg(TEAM_COLOR));
    }

    private void resetModeImages() {
        imgSolo.setImage(loadImg(SOLO_GRAY));
        imgTeam.setImage(loadImg(TEAM_GRAY));
    }

    /* ==================== 인원 선택 (2인 / 4인) ==================== */

    /** 2인 버튼 클릭 */
    @FXML
    private void handleSelectSize2() {
        isTeamMode = false;
        playerCount = 2;

        resetModeImages();
        imgSolo.setImage(loadImg(SOLO_COLOR));
        imgTeam.setImage(loadImg(TEAM_GRAY));

        resetSizeImages();
        imgSize2.setImage(loadImg(SIZE2_COLOR));
    }

    /** 4인 버튼 클릭 */
    @FXML
    private void handleSelectSize4() {
        isTeamMode = false;
        playerCount = 4;

        resetModeImages();
        imgSolo.setImage(loadImg(SOLO_COLOR));
        imgTeam.setImage(loadImg(TEAM_GRAY));

        resetSizeImages();
        imgSize4.setImage(loadImg(SIZE4_COLOR));
    }

    private void resetSizeImages() {
        imgSize2.setImage(loadImg(SIZE2_GRAY));
        imgSize4.setImage(loadImg(SIZE4_GRAY));
    }

    /* ==================== 규칙 보기 ==================== */

    @FXML
    private void handleShowRules() {
        SceneRouter.go("/fxml/splash/RulesView.fxml");
    }

    /* ==================== 게임 시작 ==================== */

    @FXML
    private void handleStartGame() {

        if (isTeamMode) {
            playerCount = 4; // 2:2
            System.out.println("게임 시작: mode=team, count=" + playerCount);
            SceneRouter.go("/fxml/game/GameIntroView.fxml");
            return;
        }

        if (playerCount == 0) {
            System.out.println("[WARN] 개인전 인원을 선택하지 않았습니다.");
            return;
        }

        String mode = (playerCount == 2) ? "1v1" : "1v1v1v1";
        System.out.println("게임 시작: mode=" + mode + ", count=" + playerCount);

        SceneRouter.go("/fxml/lobby/MatchingView.fxml");
    }

    /* ==================== 유틸 ==================== */

    private Image loadImg(String path) {
        URL url = getClass().getResource(path);
        if (url == null) {
            System.out.println("[WARN] 이미지 리소스를 찾을 수 없음: " + path);
            return null;
        }
        return new Image(url.toExternalForm());
    }

    // setUserInfo / setStats 는 이제 안 써도 되지만, 다른 곳에서 호출 중이면 그대로 둬도 괜찮음
}