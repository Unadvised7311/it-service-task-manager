package com.danny.taskmanager.dto;

public record InvitationPreviewDto(
        boolean valid,
        String projectName,
        String accessLevel,
        String email,
        String reason
) {}
