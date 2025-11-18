/** CardType
 * 역할: 카드 종류 Enum(8종).
 * 핵심기능: 이름/설명/아이콘 키 등 메타 제공.
 */

package com.omokpang.domain.card;

public enum CardType {

    REMOVE("/images/cards/la_Remove.png", 15),
    DOUBLE_MOVE("/images/cards/la_DoubleMove.png", 4),
    SWAP("/images/cards/la_Swap.png", 4),
    TIME_LOCK("/images/cards/la_TimeLock.png", 15),
    DEFENSE("/images/cards/la_Defense.png", 20),
    SHIELD("/images/cards/la_Shield.png", 15),
    SHARED_STONE("/images/cards/la_SharedStone.png", 15),
    BOMB("/images/cards/la_Bomb.png", 12);

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
