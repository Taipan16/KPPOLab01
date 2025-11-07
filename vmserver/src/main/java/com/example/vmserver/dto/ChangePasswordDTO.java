package com.example.vmserver.dto;

public record ChangePasswordDTO(String Current_password,
String new_password,
String new_password_again) {

}
