package com.danny.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberAssignment(
        @NotNull Long userId,
        @NotBlank String accessLevel
) {}
