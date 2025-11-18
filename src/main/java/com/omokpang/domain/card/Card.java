/** Card
 * 역할: 카드(추상 기반).
 * 핵심기능: 공통 메타(타입/코스트) / 사용 템플릿 정의.
 */

package com.omokpang.domain.card;

public class Card {

    private final CardType type;

    public Card(CardType type) {
        this.type = type;
    }

    public CardType getType() {
        return type;
    }

    public String getImagePath() {
        return type.getImagePath();
    }

    public String getName() {
        return type.name();
    }
}

