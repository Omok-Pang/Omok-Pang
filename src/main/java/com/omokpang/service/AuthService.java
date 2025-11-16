/**
 * AuthService
 * 역할: 로그인/회원가입 → PostgreSQL 연동 버전
 */
package com.omokpang.service;

import com.omokpang.domain.user.User;
import com.omokpang.repository.DataSourceProvider;
import java.sql.*;
import java.time.LocalDateTime;

public class AuthService {

    // 회원가입
    public boolean signup(String nickname, String pw) {
        String sql = "INSERT INTO users (nickname, password) VALUES (?, ?)";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nickname);
            pstmt.setString(2, pw);

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            // UNIQUE(nickname) 충돌 시
            System.out.println("회원가입 실패: " + e.getMessage());
            return false;
        }
    }

    // 로그인
    public boolean login(String nickname, String pw) {
        return loginAndGetUser(nickname, pw) != null;
    }

    // DB에서 유저 전체 정보를 가져오는 로그인 메서드
    public User loginAndGetUser(String nickname, String pw) {
        String sql = """
                SELECT id, nickname, password, wins, losses, points, created_at
                FROM users
                WHERE nickname = ? AND password = ?
                """;

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nickname);
            pstmt.setString(2, pw);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null; // 아이디/비번 안 맞음
                }

                int id = rs.getInt("id");
                String nick = rs.getString("nickname");
                String storedPw = rs.getString("password");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                int points = rs.getInt("points");
                Timestamp ts = rs.getTimestamp("created_at");
                LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;

                return new User(id, nick, storedPw, wins, losses, points, createdAt);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
