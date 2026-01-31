package com.company.meeting.test;

import com.company.meeting.common.db.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;

/**
 * DB 연결 테스트용 Servlet
 * - 성공 시: DB CONNECT OK
 * - 실패 시: 에러 출력
 */
@WebServlet("/db-test")
public class DBTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        resp.setContentType("text/plain; charset=UTF-8");

        try (Connection conn = DBConnection.getConnection()) {
            resp.getWriter().write("DB CONNECT OK");
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("DB CONNECT FAIL");
        }
    }
}
