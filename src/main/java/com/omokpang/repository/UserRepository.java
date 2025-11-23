package com.omokpang.repository;

import com.omokpang.domain.user.User;

import java.util.List;
import java.util.ArrayList;

import java.sql.*;
import java.time.LocalDateTime;

/** UserRepository
 * 역할: users 테이블에 대한 CRUD 및 조회 전담 저장소.
 * 핵심기능: 회원가입/로그인/닉네임 중복 검사/포인트 랭킹 조회 제공.
 */
public class UserRepository {

    // 닉네임 중복 여부 확인
    public boolean existsByNickname(String nickname) {
        String sql = "SELECT 1 FROM users WHERE nickname = ?";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nickname);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // row 하나라도 있으면 true
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // 에러일 때는 일단 "없다"로 처리 (필요하면 로깅/예외 분리 가능)
            return false;
        }
    }

    // 회원가입: 새 유저 저장
    public boolean save(String nickname, String password) {
        // wins / losses / points는 DEFAULT 0 이라 컬럼에서 생략 가능
        String sql = "INSERT INTO users (nickname, password) VALUES (?, ?)";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nickname);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            // 닉네임 UNIQUE 제약 위반 시 SQLState 23505
            if ("23505".equals(e.getSQLState())) {
                return false;
            }
            e.printStackTrace();
            return false;
        }
    }

    // 로그인: 닉네임 + 비밀번호로 유저 검색
    public User findByNicknameAndPassword(String nickname, String password) {
        String sql = """
                SELECT id, nickname, password, wins, losses, points, created_at
                FROM users
                WHERE nickname = ? AND password = ?
                """;

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nickname);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                int id = rs.getInt("id");
                String nick = rs.getString("nickname");
                String pw = rs.getString("password");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                int points = rs.getInt("points");
                Timestamp ts = rs.getTimestamp("created_at");
                LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;

                return new User(id, nick, pw, wins, losses, points, createdAt);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<User> findAllOrderByPoints() {
        String sql = """
    SELECT id, nickname, password, wins, losses, points, created_at
    FROM users
    ORDER BY points DESC
    """;

        List<User> list = new ArrayList<>();

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String nickname = rs.getString("nickname");
                String pw = rs.getString("password");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                int points = rs.getInt("points");
                Timestamp ts = rs.getTimestamp("created_at");
                LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;

                list.add(new User(id, nickname, pw, wins, losses, points, createdAt));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
}

