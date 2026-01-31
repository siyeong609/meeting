package com.company.meeting.common.db;

import com.mysql.jdbc.Driver;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * JDBC 공용 DB 연결 클래스
 * - 모든 DAO에서 공통으로 사용
 * - Tomcat 10 + MySQL 기준
 */
public class DBConnection {

    // Docker MySQL 접속 정보
    private static final String URL =
            "jdbc:mysql://localhost:3306/meeting_room?serverTimezone=Asia/Seoul";

    private static final String USER = "root";
    private static final String PASSWORD = "1234";

    /**
     * DB 커넥션 반환
     */
    public static Connection getConnection() throws Exception {
        DriverManager.registerDriver(new Driver());
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
