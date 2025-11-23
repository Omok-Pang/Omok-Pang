package com.omokpang.service;

import com.omokpang.repository.DataSourceProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** UserPointService
 * 역할: users 테이블의 포인트 조회·차감을 전담하는 서비스.
 * 핵심기능: 현재 포인트 조회(getPoint) 및 조건부 차감(decreasePoint) 처리.
 */
public class UserPointService {

    // 현재 포인트 조회 (없으면 0)
    public int getPoint(long userId) {
        String sql = "SELECT points FROM users WHERE id = ?";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("points");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 포인트 차감
     *  - points >= amount 인 경우에만 차감
     *  - 성공 시 true / 실패 시 false
     */
    public boolean decreasePoint(long userId, int amount) {
        String sql =
                "UPDATE users SET points = points - ? " +
                        "WHERE id = ? AND points >= ?";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, amount);
            ps.setLong(2, userId);
            ps.setInt(3, amount);

            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
