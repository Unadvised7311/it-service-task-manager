package com.danny.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Der Name hier MUSS exakt wie der Dateiname sein
public record TaskRequest(
    @NotBlank String title,
    String description,
    @NotNull Long projectId
) {}
