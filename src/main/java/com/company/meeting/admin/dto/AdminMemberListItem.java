package com.company.meeting.admin.dto;

/**
 * 관리자 회원 목록/팝업에 필요한 최소 필드
 * - 민감정보(password 등) 포함 금지
 */
public class AdminMemberListItem {

    private final int id;
    private final String loginId;
    private final String name;
    private final String email;
    private final String createdAt;
    private final String profileImage;
    private final String memo;

    // ✅ 추가: role (ADMIN/USER)
    private final String role;

    public AdminMemberListItem(
            int id,
            String loginId,
            String name,
            String email,
            String createdAt,
            String profileImage,
            String role,
            String memo
    ) {
        this.id = id;
        this.loginId = loginId;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
        this.profileImage = profileImage;
        this.role = role;
        this.memo = memo;
    }

    public int getId() {
        return id;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getRole() {
        return role;
    }

    public String getMemo() {
        return memo;
    }
}
