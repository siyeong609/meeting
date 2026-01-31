package com.company.meeting.user.dao;

import com.company.meeting.common.db.DBConnection;
import com.company.meeting.user.dto.UserDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 사용자 관련 DB 접근 객체
 */
public class UserDAO {

    /**
     * 로그인 ID로 사용자 조회
     */
    public UserDTO findByLoginId(String loginId) {

        String sql = "SELECT * FROM user WHERE login_id = ?";
        UserDTO user = null;

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, loginId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                user = new UserDTO();
                user.setId(rs.getInt("id"));
                user.setLoginId(rs.getString("login_id"));
                user.setPassword(rs.getString("password"));
                user.setName(rs.getString("name"));
                user.setRole(rs.getString("role"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return user;
    }
}
