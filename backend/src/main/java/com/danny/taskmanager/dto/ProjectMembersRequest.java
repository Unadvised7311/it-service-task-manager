package com.danny.taskmanager.dto;

import jakarta.validation.Valid;

import java.util.List;

public record ProjectMembersRequest(
        @Valid List<MemberAssignment> members
) {
    public ProjectMembersRequest {
        members = members != null ? members : List.of();
    }
}
