/**
 * AuthService
 * 역할: 로그인/회원가입 → PostgreSQL 연동 버전
 */
package com.omokpang.service;

import com.omokpang.repository.DataSourceProvider;
import java.sql.*;

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
        String sql = "SELECT password FROM users WHERE nickname = ?";

        try (Connection conn = DataSourceProvider.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nickname);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPw = rs.getString("password");
                return pw.equals(storedPw);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
