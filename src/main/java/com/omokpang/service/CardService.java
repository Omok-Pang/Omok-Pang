package com.omokpang.service;

import com.omokpang.domain.card.Card;
import com.omokpang.domain.card.CardType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** CardService
 * 역할: 카드 뽑기·리롤 정책을 담당하는 서비스.
 * 핵심기능: CardType 가중치 기반 랜덤 카드 생성 및 2장 지급 로직 제공.
 */
public class CardService {

    private static final Random rand = new Random();

    // 2장 뽑기
    public List<Card> drawTwo() {
        List<Card> list = new ArrayList<>();
        list.add(drawOne());
        list.add(drawOne());
        return list;
    }

    // 리롤
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
