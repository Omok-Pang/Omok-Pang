package com.omokpang.controller.lobby;

import com.omokpang.SceneRouter;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Button;

public class MatchingController {

    @FXML
    private ImageView myAvatar;

    @FXML
    private ImageView cancelButtonImage;   // ❗ FXML에서 넣을 이미지뷰

    @FXML
    private Button cancelBtn;

    @FXML
    public void initialize() {

        // ✅ 기존 아바타 이미지 로딩
        Image avatar = new Image(
                getClass().getResource("/images/user/user3.png").toExternalForm()
        );
        myAvatar.setImage(avatar);


        // ============================
        //   🔥 취소 버튼 이미지 적용
        // ============================
        Image normal = new Image(
                getClass().getResource("/images/button/match_btn.png").toExternalForm()
        );

        Image hover = new Image(
                getClass().getResource("/images/button/match_btn.png").toExternalForm()
        );

        cancelButtonImage.setImage(normal);

        // Hover 효과
        cancelButtonImage.setOnMouseEntered(e -> cancelButtonImage.setImage(hover));
        cancelButtonImage.setOnMouseExited(e -> cancelButtonImage.setImage(normal));
    }

    @FXML
    private void onCancel() {
        SceneRouter.go("/fxml/main/MainView.fxml");
    }
}
