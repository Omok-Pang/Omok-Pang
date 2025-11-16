/** DataSourceProvider
 * 역할: PostgreSQL DataSource 제공(HikariCP).
 * 핵심기능: 연결 풀 설정 로드 / 싱글턴 제공.
 */

package com.omokpang.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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