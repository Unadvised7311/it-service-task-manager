package com.danny.taskmanager.dto;

public record AuthResponse(String token, UserSummaryDto user) {}
