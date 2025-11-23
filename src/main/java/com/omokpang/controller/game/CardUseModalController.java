/** CardUseModalController : 카드 사용 선택 모달 컨트롤러.
 * 역할: 보유 카드 손패를 크게 렌더링하고, 선택된 카드를 GameBoardController에 콜백으로 전달.
 * 핵심기능: 카드 이미지(me_/sm_ → la_) 변환 / 버튼 클릭 시 카드 선택·모달 닫기 / centerStack에서 자기 자신 제거.
 */

package com.omokpang.controller.game;

import com.omokpang.domain.card.Card;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CardUseModalController {

    @FXML
    private StackPane root;

    @FXML
    private HBox handCardBox;

    // 현재 화면에 표시할 카드들
    private List<Card> cards = new ArrayList<>();

    // 카드 선택 콜백 : 어떤 카드를 골랐는지 GameBoard에 알려줌
    private Consumer<Card> onCardSelected;

    // GameBoardController 에서 카드 리스트 세팅
    public void setCards(List<Card> cards) {
        this.cards.clear();
        if (cards != null) {
            this.cards.addAll(cards);
        }
        renderCards();
    }

    // 카드 선택 시 호출할 콜백 : 선택된 Card 전달
    public void setOnCardSelected(Consumer<Card> onCardSelected) {
        this.onCardSelected = onCardSelected;
    }

    // handCardBox 안에 보유 카드들을 la_이미지로 렌더링
    private void renderCards() {
        if (handCardBox == null) return;

        handCardBox.getChildren().clear();

        if (cards == null || cards.isEmpty()) {
            return;
        }

        for (Card card : cards) {
            String basePath = card.getImagePath(); // /images/gamecard/me_*.png
            String largePath = toLargeImagePath(basePath); // /images/gamecard/la_*.png 으로 변환

            Image img = new Image(
                    getClass().getResource(largePath).toExternalForm()
            );
            ImageView iv = new ImageView(img);
            iv.setFitHeight(220);
            iv.setPreserveRatio(true);

            Button btn = new Button();
            btn.setStyle("-fx-background-color: transparent; -fx-padding: 0;");
            btn.setGraphic(iv);

            btn.setOnAction(e -> handleCardClick(card));

            handCardBox.getChildren().add(btn);
        }
    }

    // me_/sm_ 경로를 la_ 경로로 변환
    private String toLargeImagePath(String path) {
        if (path == null) {
            return "/images/gamecard/la_SharedStone.png";
        }
        // 이미 la_면 그대로 사용
        if (path.contains("/la_")) {
            return path;
        }
        // me_ → la_, sm_ → la_
        String converted = path.replace("/me_", "/la_")
                .replace("/sm_", "/la_");
        return converted;
    }

    // ───────────────── 카드 선택/닫기 ─────────────────

    // 카드 버튼 클릭 시
    private void handleCardClick(Card card) {
        if (onCardSelected != null) {
            onCardSelected.accept(card);   // 선택된 카드 전달
        }
        close();
    }

    // 닫기 버튼 클릭 시
    @FXML
    private void handleClose() {
        close();
    }

    // 실제 닫기 로직: centerStack 에서 자기 자신 제거
    private void close() {
        if (root == null) return;

        if (root.getParent() instanceof Pane parent) {
            parent.getChildren().remove(root);
        }
    }
}
