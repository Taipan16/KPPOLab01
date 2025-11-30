package com.example.vmserver.dto;

public record PasswordResetRequest(String oldPassword, String newPassword) {

}
