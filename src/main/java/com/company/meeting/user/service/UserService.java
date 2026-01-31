package com.company.meeting.user.service;

import com.company.meeting.user.dao.UserDAO;
import com.company.meeting.user.dto.UserDTO;

/**
 * User 비즈니스 로직 담당 Service
 * - Controller(Servlet)는 이 클래스만 호출
 * - DAO 직접 접근 금지
 * - 추후 트랜잭션, 인증, 정책 로직이 이곳에 추가됨
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /**
     * 로그인 ID로 사용자 조회
     * - Controller 단에서는 null 여부만 판단
     */
    public UserDTO getUserByLoginId(String loginId) {
        return userDAO.findByLoginId(loginId);
    }

    /**
     * 로그인 처리
     * - 현재는 단순 비교
     * - 추후 비밀번호 암호화, 실패 횟수, 잠금 정책 추가 예정
     */
    public boolean login(String loginId, String password) {

        UserDTO user = userDAO.findByLoginId(loginId);

        if (user == null) {
            return false;
        }

        // 단순 평문 비교 (현재 단계)
        return user.getPassword().equals(password);
    }

    /**
     * 전체 회원 수 반환
     */
    public int getTotalUserCount() {
        return userDAO.countAllUsers();
    }
}
