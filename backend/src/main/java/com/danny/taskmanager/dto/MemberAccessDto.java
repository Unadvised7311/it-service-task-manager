package com.danny.taskmanager.dto;

public class MemberAccessDto {
    private Long userId;
    private String username;
    private String accessLevel;

    public MemberAccessDto() {}

    public MemberAccessDto(Long userId, String username, String accessLevel) {
        this.userId = userId;
        this.username = username;
        this.accessLevel = accessLevel;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
}
