package com.example.vmserver.dto;

public record RegisterRequestDTO(
    String username,
    String password,
    String confirmPassword
) {}
