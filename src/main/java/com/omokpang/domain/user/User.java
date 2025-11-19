/** User
 * 역할: 사용자 엔티티.
 * 핵심기능: 계정정보 보유 / 승·패 시 포인트 갱신.
 */

package com.omokpang.domain.user;

import java.time.LocalDateTime;

public class User {

    private final int id;
    private final String nickname;
    private final String password;
    private final int wins;
    private final int losses;
    private final int points;
    private final LocalDateTime createdAt;

    public User(int id,
                String nickname,
                String password,
                int wins,
                int losses,
                int points,
                LocalDateTime createdAt) {
        this.id = id;
        this.nickname = nickname;
        this.password = password;
        this.wins = wins;
        this.losses = losses;
        this.points = points;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }

    public String getNickname() { return nickname; }

    public String getPassword() { return password; }

    public int getWins() { return wins; }

    public int getLosses() { return losses; }

    public int getPoints() { return points; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

