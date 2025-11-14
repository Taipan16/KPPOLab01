package com.example.vmserver.service;

import org.springframework.http.ResponseEntity;

import com.example.vmserver.dto.LoginRequestDTO;
import com.example.vmserver.dto.LoginResponseDTO;
import com.example.vmserver.dto.RegisterRequestDTO;
import com.example.vmserver.dto.ResetPasswordDTO;
import com.example.vmserver.dto.VMUserLoggedDTO;

public interface AuthenticationService {
    ResponseEntity<LoginResponseDTO> login(LoginRequestDTO request, String access, String refresh);
    ResponseEntity<LoginResponseDTO> refresh(String refresh);
    ResponseEntity<LoginResponseDTO> logout(String access, String refresh);
    ResponseEntity<LoginResponseDTO> resetPassword(ResetPasswordDTO request, String access, String refresh);
    ResponseEntity<LoginResponseDTO> register(RegisterRequestDTO request);
    VMUserLoggedDTO info();
}
