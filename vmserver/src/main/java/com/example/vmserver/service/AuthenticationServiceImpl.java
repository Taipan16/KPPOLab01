package com.example.vmserver.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.vmserver.dto.LoginRequestDTO;
import com.example.vmserver.dto.LoginResponseDTO;
import com.example.vmserver.dto.RegisterRequestDTO;
import com.example.vmserver.dto.ResetPasswordDTO;
import com.example.vmserver.dto.VMUserLoggedDTO;
import com.example.vmserver.exception.ResourceNotFoundException;
import com.example.vmserver.jwt.JwtTokenProvider;
import com.example.vmserver.mapper.VMUserMapper;
import com.example.vmserver.model.Token;
import com.example.vmserver.model.VMUser;
import com.example.vmserver.repository.TokenRepository;
import com.example.vmserver.util.CookieUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);
    
    private final TokenRepository tokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final AuthenticationManager authenticationManager;
    private final VMUserService vmUserService;

    @Value("${jwt.access.duration.minutes}")
    private long accessDurationMin;
    @Value("${jwt.access.duration.second}")
    private long accessDurationSec;
    @Value("${jwt.refresh.duration.day}")
    private long refreshDurationDate;
    @Value("${jwt.refresh.duration.second}")
    private long refreshDurationSec;

    private void addAccessTokenCookie(HttpHeaders headers, Token token){
        headers.add(HttpHeaders.SET_COOKIE, cookieUtil.createAccessCookie(token.getValue(), accessDurationSec).toString());
    }

    private void addRefreshTokenCookie(HttpHeaders headers, Token token){
        headers.add(HttpHeaders.SET_COOKIE, cookieUtil.createRefreshCookie(token.getValue(), refreshDurationSec).toString());
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

    @Override
    public ResponseEntity<LoginResponseDTO> login(LoginRequestDTO request, String access, String refresh) {
        logger.info("Начало выполнения метода login для пользователя: {}", request.username());
        
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.username(), request.password()));
            VMUser user = vmUserService.getVMUser(request.username());

            boolean accessValid = jwtTokenProvider.isValid(access);
            boolean refreshValid = jwtTokenProvider.isValid(refresh);

            HttpHeaders headers = new HttpHeaders();

            revokeAllTokens(user);

            if (!accessValid) {
                Token newAccess = jwtTokenProvider.generatedAccessToken(Map.of("role", user.getRole().getAuthority()),
                accessDurationMin, ChronoUnit.MINUTES, user);

                newAccess.setVmUser(user);
                addAccessTokenCookie(headers, newAccess);
                tokenRepository.save(newAccess);
                logger.debug("Создан новый access token для пользователя: {}", request.username());
            }

            if (!refreshValid || accessValid) {
                Token newRefresh = jwtTokenProvider.generatedRefreshToken(refreshDurationDate, ChronoUnit.DAYS, user);

                newRefresh.setVmUser(user);
                addRefreshTokenCookie(headers, newRefresh);
                tokenRepository.save(newRefresh);
                logger.debug("Создан новый refresh token для пользователя: {}", request.username());
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.info("Метод login успешно выполнен для пользователя: {}", request.username());
            return ResponseEntity.ok().headers(headers).body(new LoginResponseDTO(true, user.getRole().getName()));
        } catch (Exception e) {
            logger.error("Ошибка в методе login для пользователя: {}. Причина: {}", request.username(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ResponseEntity <LoginResponseDTO> refresh(String refreshToken)
    {
        logger.info("Начало выполнения метода refresh");
        
        try {
            if(!jwtTokenProvider.isValid(refreshToken)){
                logger.warn("Недействительный refresh token");
                throw new RuntimeException("token is invalid");
            }
            
            String username = jwtTokenProvider.getVMUserName(refreshToken);
            logger.debug("Обновление токена для пользователя: {}", username);
            
            VMUser user = vmUserService.getVMUser(username);

            Token newAccess = jwtTokenProvider.generatedAccessToken(Map.of("role", user.getRole().getAuthority()), accessDurationMin, ChronoUnit.MINUTES, user);

            newAccess.setVmUser(user);
            HttpHeaders headers = new HttpHeaders();
            addAccessTokenCookie(headers, newAccess);

            tokenRepository.save(newAccess);

            logger.info("Метод refresh успешно выполнен для пользователя: {}", username);
            return ResponseEntity.ok().headers(headers).body(new LoginResponseDTO(true, user.getRole().getName()));
        } catch (Exception e) {
            logger.error("Ошибка в методе refresh. Причина: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ResponseEntity <LoginResponseDTO> logout(String accessToken, String refresh){
        logger.info("Начало выполнения метода logout");
        
        try {
            String username = jwtTokenProvider.getVMUserName(accessToken);
            logger.debug("Выход пользователя: {}", username);
            
            SecurityContextHolder.clearContext();
            VMUser user = vmUserService.getVMUser(username);
            revokeAllTokens(user);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
            headers.add(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString());

            logger.info("Метод logout успешно выполнен для пользователя: {}", username);
            return ResponseEntity.ok().headers(headers).body(new LoginResponseDTO(false, null));
        } catch (Exception e) {
            logger.error("Ошибка в методе logout. Причина: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public VMUserLoggedDTO info(){
        logger.info("Начало выполнения метода info");
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if(authentication instanceof AnonymousAuthenticationToken){
                logger.warn("Попытка получения информации о неаутентифицированном пользователе");
                throw new RuntimeException("No user");
            }

            String username = authentication.getName();
            logger.debug("Получение информации о пользователе: {}", username);
            
            VMUser user = vmUserService.getVMUser(username);

            logger.info("Метод info успешно выполнен для пользователя: {}", username);
            return VMUserMapper.userToUserLoggedDto(user);
        } catch (Exception e) {
            logger.error("Ошибка в методе info. Причина: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ResponseEntity<LoginResponseDTO> resetPassword(ResetPasswordDTO request, String access, String refresh){
        logger.info("Начало выполнения метода resetPassword");
        
        try {
            String username = jwtTokenProvider.getVMUserName(access);
            logger.debug("Смена пароля для пользователя: {}", username);
            
            VMUser user = vmUserService.getVMUser(username);
            
            vmUserService.resetPassword(user.getUsername(), request.oldPassword(), request.newPassord());
            
            logger.info("Метод resetPassword успешно выполнен для пользователя: {}", username);
            return logout(access, refresh);
        } catch (Exception e) {
            logger.error("Ошибка в методе resetPassword. Причина: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public ResponseEntity<LoginResponseDTO> register(RegisterRequestDTO request) {
        logger.info("Начало выполнения метода register для пользователя: {}", request.username());
        
        try {
            // Проверка совпадения паролей
            if (!request.password().equals(request.confirmPassword())) {
                logger.warn("Пароли не совпадают для пользователя: {}", request.username());
                throw new RuntimeException("Пароли не совпадают");
            }
            
            // Проверка существования пользователя
            try {
                vmUserService.getVMUser(request.username());
                logger.warn("Попытка регистрации существующего пользователя: {}", request.username());
                throw new RuntimeException("Данный пользователь уже существует");
            }
            catch (ResourceNotFoundException e) {
                // Пользователь не существует - продолжаем
                logger.debug("Пользователь {} не существует, можно продолжить регистрацию", request.username());
            }
            
            // Создание нового пользователя - теперь возвращает DTO
            vmUserService.createUser(request.username(), request.password());
            logger.debug("Пользователь {} успешно создан", request.username());
            
            // Аутентификация нового пользователя
            LoginRequestDTO loginRequest = new LoginRequestDTO(request.username(), request.password());
            
            logger.info("Метод register успешно выполнен для пользователя: {}", request.username());
            return login(loginRequest, null, null);
        } catch (Exception e) {
            logger.error("Ошибка в методе register для пользователя: {}. Причина: {}", request.username(), e.getMessage(), e);
            throw e;
        }
    }
}