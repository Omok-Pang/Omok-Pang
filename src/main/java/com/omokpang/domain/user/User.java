package com.omokpang.domain.user;

import java.time.LocalDateTime;

/** User
 * 역할: users 테이블과 매핑되는 도메인 사용자 엔티티.
 * 핵심기능: 닉네임·전적·포인트 등 계정 정보를 불변 객체로 보유.
 */
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

