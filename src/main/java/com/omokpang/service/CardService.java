/** CardService
 * 역할: 카드 지급·리롤·보유 정책.
 * 핵심기능: 2장 랜덤 지급 / 40pt 리롤 / 사용 가능 여부 검증.
 */

package com.omokpang.service;

import com.omokpang.domain.card.Card;
import com.omokpang.domain.card.CardType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CardService {

    private static final Random rand = new Random();

    /** 2장 뽑기 */
    public List<Card> drawTwo() {
        List<Card> list = new ArrayList<>();
        list.add(drawOne());
        list.add(drawOne());
        return list;
    }

    /** 리롤 */
    public Card drawOne() {
        int total = 0;

        for (CardType t : CardType.values())
            total += t.getWeight();

        int r = rand.nextInt(total) + 1;
        int sum = 0;

        for (CardType t : CardType.values()) {
            sum += t.getWeight();
            if (r <= sum)
                return new Card(t);
        }
        return new Card(CardType.REMOVE);
    }
}
