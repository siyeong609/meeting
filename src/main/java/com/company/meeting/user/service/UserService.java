package com.company.meeting.user.service;

import com.company.meeting.admin.dto.AdminMemberListItem;
import com.company.meeting.common.util.paging.PageInfo;
import com.company.meeting.common.util.paging.PageRequest;
import com.company.meeting.common.util.security.PasswordUtil;
import com.company.meeting.user.dao.UserDAO;
import com.company.meeting.user.dto.UserDTO;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * User 비즈니스 로직 담당 Service
 * - Servlet(Controller)는 이 클래스만 호출
 * - DAO 직접 접근 금지
 * - 인증/정책/검증/가공 로직은 여기로 모은다
 */
public class UserService {

    private final UserDAO userDAO;

    // 날짜 표기 통일 (목록용)
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public UserService() {
        this.userDAO = new UserDAO();
    }

    // =========================================================
    // 1) 로그인/인증
    // =========================================================

    /**
     * 인증(로그인)
     * - 성공: UserDTO 반환
     * - 실패: null 반환
     * <p>
     * 정책:
     * - DB 저장값이 해시면 해시 검증
     * - DB 저장값이 평문이면 평문 비교 후 성공 시 해시로 업그레이드
     */
    public UserDTO authenticate(String loginId, String plainPassword) {

        if (loginId == null || loginId.isBlank()) return null;
        if (plainPassword == null) plainPassword = "";

        UserDTO user = userDAO.findByLoginId(loginId.trim());
        if (user == null) return null;

        String stored = user.getPassword();
        boolean ok = PasswordUtil.verify(plainPassword, stored);

        if (!ok) return null;

        // 평문 저장 → 성공 시 해시로 업그레이드
        if (!PasswordUtil.isHashed(stored)) {
            String hashed = PasswordUtil.hash(plainPassword);
            userDAO.updatePasswordHash(user.getId(), hashed);
            user.setPassword(hashed);
        }

        return user;
    }

    /**
     * 로그인 성공 여부만 필요한 경우
     */
    public boolean login(String loginId, String plainPassword) {
        return authenticate(loginId, plainPassword) != null;
    }

    /**
     * ✅ (기존 코드 호환) 로그인ID로 사용자 조회
     * - AdminLoginServlet 등에서 사용
     */
    public UserDTO getUserByLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) return null;
        return userDAO.findByLoginId(loginId.trim());
    }

    // =========================================================
    // 2) 대시보드용 통계
    // =========================================================

    /**
     * 전체 유저 수(ADMIN 포함/제외 정책은 DAO 쿼리로 제어)
     */
    public int getTotalUserCount() {
        return userDAO.countAllUsers();
    }

    // =========================================================
    // 3) 관리자: 회원 목록(검색/페이징) - JSON 렌더링용
    // =========================================================

    /**
     * (관리자) 유저 목록 페이징 + 검색
     * - ADMIN 포함
     * - 목록에서는 password 같은 민감정보 제외 DTO로 변환
     */
    public AdminMemberPageResult getAdminMembers(String q, PageRequest pr) {

        long total = userDAO.countUsersByQuery(q);
        List<UserDTO> users = userDAO.findUsersByQuery(q, pr.getOffset(), pr.getSize());

        List<AdminMemberListItem> items = new ArrayList<>();

        for (UserDTO u : users) {
            String createdAt = (u.getCreatedAt() == null) ? "" : u.getCreatedAt().format(DATE_FMT);
            String email = (u.getEmail() == null) ? "" : u.getEmail();
            String profileImage = (u.getProfileImage() == null) ? "" : u.getProfileImage();
            String role = (u.getRole() == null) ? "" : u.getRole();
            String memo = (u.getMemo() == null) ? "" : u.getMemo();

            // ✅ AdminMemberListItem 생성자 시그니처에 맞춰야 함(role 포함 버전)
            items.add(new AdminMemberListItem(
                    u.getId(),
                    u.getLoginId(),
                    u.getName(),
                    email,
                    createdAt,
                    profileImage,
                    role,
                    memo
            ));
        }

        PageInfo pageInfo = new PageInfo(pr.getPage(), pr.getSize(), total);
        return new AdminMemberPageResult(items, pageInfo);
    }

    /**
     * 관리자 회원 목록 응답 묶음(data + page)
     */
    public static class AdminMemberPageResult {
        private final List<AdminMemberListItem> data;
        private final PageInfo page;

        public AdminMemberPageResult(List<AdminMemberListItem> data, PageInfo page) {
            this.data = data;
            this.page = page;
        }

        public List<AdminMemberListItem> getData() {
            return data;
        }

        public PageInfo getPage() {
            return page;
        }
    }

    // =========================================================
    // 4) 관리자: 회원 생성/삭제
    // =========================================================

    /**
     * 관리자: 회원 생성
     * - password는 해시 저장
     * - role은 USER/ADMIN만 허용 (기본 USER)
     * - profile_image는 NULL로 두는 정책(프론트에서 NULL이면 default 렌더)
     *
     * @return 생성된 userId(PK)
     */
    public int createMember(String loginId, String plainPassword, String name, String email, String role) {

        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("아이디(loginId)는 필수입니다.");
        }
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호(password)는 필수입니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름(name)은 필수입니다.");
        }

        String r = (role == null || role.isBlank()) ? "USER" : role.trim().toUpperCase();
        if (!"USER".equals(r) && !"ADMIN".equals(r)) {
            throw new IllegalArgumentException("role은 USER 또는 ADMIN만 가능합니다.");
        }

        // email: 빈 문자열이면 null
        if (email != null && email.isBlank()) email = null;

        String hashed = PasswordUtil.hash(plainPassword);

        // DAO에서 profile_image를 NULL로 저장하도록 구현돼 있어야 함
        return userDAO.insertUser(loginId.trim(), hashed, name.trim(), email, r);
    }

    /**
     * 관리자: 회원 다중 삭제
     * - ADMIN 삭제는 DAO에서 차단(role='USER' 조건)
     * - 로그인한 관리자 자신 삭제 방지
     *
     * @param userIds      삭제 대상 user.id 리스트
     * @param loginAdminId 현재 로그인한 관리자 user.id
     */
    public DeleteResult deleteMembers(List<Integer> userIds, int loginAdminId) {

        int deleted = 0;
        List<Integer> skipped = new ArrayList<>();
        List<Integer> failed = new ArrayList<>();

        if (userIds == null || userIds.isEmpty()) {
            return new DeleteResult(0, skipped, failed);
        }

        for (Integer id : userIds) {
            if (id == null) continue;

            // 자기 자신 삭제 방지
            if (id == loginAdminId) {
                skipped.add(id);
                continue;
            }

            try {
                int n = userDAO.deleteUserById(id);

                if (n == 1) {
                    deleted++;
                } else {
                    // ADMIN이거나 존재하지 않거나 삭제 불가
                    skipped.add(id);
                }
            } catch (RuntimeException e) {
                // FK 제약(예약 등)으로 실패 가능
                failed.add(id);
            }
        }

        return new DeleteResult(deleted, skipped, failed);
    }

    public static class DeleteResult {
        private final int deletedCount;
        private final List<Integer> skippedIds;
        private final List<Integer> failedIds;

        public DeleteResult(int deletedCount, List<Integer> skippedIds, List<Integer> failedIds) {
            this.deletedCount = deletedCount;
            this.skippedIds = skippedIds;
            this.failedIds = failedIds;
        }

        public int getDeletedCount() {
            return deletedCount;
        }

        public List<Integer> getSkippedIds() {
            return skippedIds;
        }

        public List<Integer> getFailedIds() {
            return failedIds;
        }
    }

    // =========================================================
    // 5) 공용: 프로필/이메일/비밀번호 수정
    // =========================================================

    /**
     * 비밀번호 변경(관리자/사용자 공용)
     * - PBKDF2 해시로 저장
     */
    public void changePassword(int userId, String newPlainPassword) {
        if (newPlainPassword == null || newPlainPassword.isBlank()) {
            throw new IllegalArgumentException("새 비밀번호가 비어있습니다.");
        }
        String hashed = PasswordUtil.hash(newPlainPassword);
        userDAO.updatePasswordHash(userId, hashed);
    }

    /**
     * 이메일 변경(빈 문자열이면 null 처리)
     */
    public void updateEmail(int userId, String email) {
        if (email != null && email.isBlank()) email = null;
        userDAO.updateEmail(userId, email);
    }

    /**
     * 프로필 이미지 경로 저장
     * - 정책: DB에는 업로드된 파일 경로만 저장 (NULL이면 디폴트 렌더)
     */
    public void updateProfileImage(int userId, String profileImagePath) {
        if (profileImagePath != null && profileImagePath.isBlank()) {
            profileImagePath = null;
        }
        userDAO.updateProfileImage(userId, profileImagePath);
    }

    /**
     * 프로필 이미지 삭제(DB NULL 처리)
     */
    public void clearProfileImage(int userId) {
        userDAO.clearProfileImage(userId);
    }

    /**
     * 프로필 이미지 경로 조회(파일 삭제를 위해 사용)
     */
    public String getProfileImagePath(int userId) {
        return userDAO.findProfileImagePath(userId);
    }

    /**
     * ✅ 관리자 메모 저장(빈 문자열이면 NULL 처리)
     */
    public void updateMemo(int userId, String memo) {
        if (memo != null && memo.isBlank()) memo = null;
        userDAO.updateMemo(userId, memo);
    }
}
