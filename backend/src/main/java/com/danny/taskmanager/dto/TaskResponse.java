package com.danny.taskmanager.dto;

public record TaskResponse(
        Long id,
        String title,
        String description,
        String status,
        Long projectId
) {}
