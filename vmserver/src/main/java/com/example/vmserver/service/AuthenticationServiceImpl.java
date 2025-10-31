package com.example.vmserver.service;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.example.vmserver.jwt.JwtTokenProvider;
import com.example.vmserver.model.Token;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.repository.TokenRepository;
import com.example.vmserver.repository.VMUserRepository;
import com.example.vmserver.util.CookieUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final VMUserRepository vmUserRepository;
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final AuthenticationManager authenticationManager;
    private final VMUserService vmUserService;

    @Value("${}")
    private long accessDurationMin;
    @Value("${}")
    private long accessDurationSec;
    @Value("${}")
    private Long refreshDurationDate;
    @Value("${}")
    private long refreshDurationSec;


    private void addAccessTokenCookie(HttpHeaders headers, Token token){
        headers.add(org.springframework.http.HttpHeaders.SET_COOKIE, cookieUtil.createAccessCookie(token.getValue(), accessDurationSec).toString());
    }

    private void addRefrashTokenCookie(HttpHeaders headers, Token token){
        headers.add(org.springframework.http.HttpHeaders.SET_COOKIE, cookieUtil.createRefreshCookie(token.getValue(), refreshDurationSec).toString());
    }

    private void revokeAllTokens(VMUser vmUser){
        Set<Token> tokens = vmUser.getTokens();
        tokens.forEach(token -> {
            if(token.getExpiringDate().isBefore(LocalDateTime.now()))
                tokenRepository.delete(token);
                else if (!token.isDisabled()){
                    token.setDisabled(true);
                    tokenRepository.save(token);
                }

        });
    }

    public ResponseEntity<LoginResponsDTO> login(LoginRequesDTO request, String access, String refresh){
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        VMUser vmuser = vmUserService.getVMUser(request.username());
        
        boolean accessValid = jwtTokenProvider.generateAccessToken(Map.of("role", vmuser.getRole().getAuthority());

    }
}
