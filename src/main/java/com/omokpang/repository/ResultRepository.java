package com.omokpang.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** ResultRepository
 * 역할: users 테이블에 게임 결과(승/패, 포인트 변화)를 반영하는 저장소.
 * 핵심기능: 닉네임 기준 wins·losses·points 컬럼 업데이트.
 */
public class ResultRepository {

    public void updateUserResult(String nickname, boolean win, int pointDelta) {
        String sql =
                "UPDATE users " +
                        "   SET wins   = wins   + ?, " +
                        "       losses = losses + ?, " +
                        "       points = points + ? " +
                        " WHERE nickname = ?";

        // 디버그 로그
        System.out.println("[DEBUG] updateUserResult called: nick=" + nickname
                + ", win=" + win + ", delta=" + pointDelta);

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, win ? 1 : 0);
            ps.setInt(2, win ? 0 : 1);
            ps.setInt(3, pointDelta);
            ps.setString(4, nickname);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
