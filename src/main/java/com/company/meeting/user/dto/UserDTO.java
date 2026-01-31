package com.company.meeting.user.dto;

import java.time.LocalDateTime;

/**
 * user 테이블 매핑 DTO
 * - DB row ↔ Java 객체 변환 전용
 * - 비즈니스 로직 없음
 */
public class UserDTO {

    private int id;
    private String loginId;
    private String password;
    private String name;
    private String role;
    private LocalDateTime createdAt;

    // ===== Getter / Setter =====

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
