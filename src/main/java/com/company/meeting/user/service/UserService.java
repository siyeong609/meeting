package com.company.meeting.user.service;

import com.company.meeting.user.dao.UserDAO;
import com.company.meeting.user.dto.UserDTO;

/**
 * 사용자 비즈니스 로직 처리
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();

    /**
     * 로그인 처리
     */
    public UserDTO login(String loginId, String password) {

        UserDTO user = userDAO.findByLoginId(loginId);

        if (user == null) {
            return null;
        }

        // ※ 지금은 평문 비교 (다음 단계에서 암호화)
        if (!user.getPassword().equals(password)) {
            return null;
        }

        return user;
    }
}
