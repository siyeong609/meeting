package com.company.meeting.common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC 공용 DB 연결 클래스
 * - 모든 DAO에서 공통으로 사용
 * - Tomcat 10 + MySQL 8 + JDK 17 기준
 * - Driver는 static block에서 1회만 등록
 */
public class DBConnection {

    // Docker MySQL 접속 정보
    private static final String URL =
            "jdbc:mysql://localhost:3306/meeting_room"
                    + "?useSSL=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=Asia/Seoul"
                    + "&characterEncoding=UTF-8";

    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    /**
     * JDBC Driver 강제 등록
     * - Smart Tomcat 환경에서 자동 로딩 실패 방지
     * - 클래스 로딩 시 1회만 실행
     */
    static {
        try {
            DriverManager.registerDriver(
                    new com.mysql.cj.jdbc.Driver()
            );
        } catch (SQLException e) {
            throw new RuntimeException("MySQL Driver 등록 실패", e);
        }
    }

    /**
     * DB 커넥션 반환
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Connection 자원 해제
     */
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // close 실패 시 추가 처리 불필요
            }
        }
    }
}
