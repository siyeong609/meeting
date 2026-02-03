package com.company.meeting.admin.dto;

/**
 * UserOption
 * - 드롭다운 옵션용 최소 DTO
 */
public class UserOption {
    private int id;
    private String name;

    public UserOption() {}

    public UserOption(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}
