package com.omokpang.domain.card;

/** CardType
 * 역할: 게임에서 사용되는 카드 종류(8종)를 정의하는 Enum.
 * 핵심기능: 각 카드의 이미지 경로와 가중치(뽑힐 확률)를 메타데이터로 제공.
 */
public enum CardType {

    REMOVE("/images/gamecard/me_Remove.png", 15),
    DOUBLE_MOVE("/images/gamecard/me_DoubleMove.png", 4),
    SWAP("/images/gamecard/me_Swap.png", 4),
    TIME_LOCK("/images/gamecard/me_TimeLock.png", 15),
    DEFENSE("/images/gamecard/me_Defense.png", 20),
    SHIELD("/images/gamecard/me_Shield.png", 15),
    SHARED_STONE("/images/gamecard/me_SharedStone.png", 12),
    BOMB("/images/gamecard/me_Bomb.png", 100);

    private final String imagePath;
    private final int weight;

    CardType(String path, int weight) {
        this.imagePath = path;
        this.weight = weight;
    }

    public String getImagePath() {
        return imagePath;
    }

    public int getWeight() {
        return weight;
    }

}
