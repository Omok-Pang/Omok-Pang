/** ResultRepository
 * 역할: users 테이블에 게임 결과(승/패, 포인트) 반영.
 */
package com.omokpang.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ResultRepository {

    /**
     * nickname 기준으로 users 테이블 업데이트
     *  - win  이면 wins + 1,  points + pointDelta
     *  - lose 이면 losses + 1, points + pointDelta
     */
    public void updateUserResult(String nickname, boolean win, int pointDelta) {
        String sql =
                "UPDATE users " +
                        "   SET wins   = wins   + ?, " +
                        "       losses = losses + ?, " +
                        "       points = points + ? " +
                        " WHERE nickname = ?";

        // 🔍 디버그 로그 추가
        System.out.println("[DEBUG] updateUserResult called: nick=" + nickname
                + ", win=" + win + ", delta=" + pointDelta);
        // 어디서 불렸는지 보고 싶으면 스택도 잠깐 찍어보기
        // new Exception("[DEBUG] stack").printStackTrace();

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
