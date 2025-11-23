package com.omokpang.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** DataSourceProvider
 * 역할: PostgreSQL JDBC 커넥션을 제공하는 유틸리티.
 * 핵심기능: 환경변수(OMOK_DB_URL/USER/PASSWORD) 기반으로 Connection 생성.
 */
public class DataSourceProvider {

    private static final String URL =
            System.getenv("OMOK_DB_URL");
    private static final String USER =
            System.getenv("OMOK_DB_USER");
    private static final String PASSWORD =
            System.getenv("OMOK_DB_PASSWORD");

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found!", e);
        }

        if (URL == null || USER == null || PASSWORD == null) {
            throw new IllegalStateException(
                    "DB 환경변수가 설정되어 있지 않습니다. " +
                            "OMOK_DB_URL / OMOK_DB_USER / OMOK_DB_PASSWORD 를 확인하세요.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}