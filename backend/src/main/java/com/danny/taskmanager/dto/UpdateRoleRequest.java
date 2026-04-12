package com.danny.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRoleRequest(@NotBlank String role) {}
