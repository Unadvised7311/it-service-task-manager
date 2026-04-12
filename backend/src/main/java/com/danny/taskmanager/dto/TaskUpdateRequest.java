package com.danny.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskUpdateRequest(
        @NotBlank String title,
        String description
) {}
