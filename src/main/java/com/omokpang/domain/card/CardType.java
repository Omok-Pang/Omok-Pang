package com.omokpang.domain.card;
/** CardType
 * 역할: 카드 종류 Enum(8종).
 * 핵심기능: 이름/설명/아이콘 키 등 메타 제공.
 */
public enum CardType {

    REMOVE("/images/gamecard/me_Remove.png", 15),
    DOUBLE_MOVE("/images/gamecard/me_DoubleMove.png", 4),
    SWAP("/images/gamecard/me_Swap.png", 4),
    TIME_LOCK("/images/gamecard/me_TimeLock.png", 15),
    DEFENSE("/images/gamecard/me_Defense.png", 20),
    SHIELD("/images/gamecard/me_Shield.png", 15),
    SHARED_STONE("/images/gamecard/me_SharedStone.png", 15),
    BOMB("/images/gamecard/me_Bomb.png", 12);

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
