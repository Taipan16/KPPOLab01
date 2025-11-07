package com.example.vmserver.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vmserver.dto.LoginRequestDTO;
import com.example.vmserver.dto.LoginResponseDTO;
import com.example.vmserver.dto.VMUserLoggedDTO;
import com.example.vmserver.service.AuthenticationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
        @CookieValue(name = "access_token", required = false) String access,
        @CookieValue(name = "refresh_token", required = false) String refresh,
        @RequestBody LoginRequestDTO request) {
        return authenticationService.login(request, access, refresh);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(
        @CookieValue(name = "access_token", required = false) String access,
        @CookieValue(name = "refresh_token", required = false) String refresh){
        return authenticationService.refresh(refresh);
    }

    @PostMapping("/logout")
    public ResponseEntity<LoginResponseDTO> logout(
        @CookieValue(name = "access_token", required = false) String access,
        @CookieValue(name = "refresh_token", required = false) String refresh){
        return authenticationService.logout(access, refresh);
    }

    @PostMapping("info")
    public ResponseEntity <VMUserLoggedDTO> info(){
        return ResponseEntity.ok(authenticationService.info());
    }
    
    
}
