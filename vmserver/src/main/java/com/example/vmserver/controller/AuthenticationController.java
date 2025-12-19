package com.example.vmserver.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.vmserver.dto.LoginRequestDTO;
import com.example.vmserver.dto.LoginResponseDTO;
import com.example.vmserver.dto.RegisterRequestDTO;
import com.example.vmserver.dto.ResetPasswordDTO;
import com.example.vmserver.dto.VMUserLoggedDTO;
import com.example.vmserver.service.AuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Контроллер аутентификации", description = "API для регистрации, авторизации и управления сессиями пользователей")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Авторизация пользователя", 
               description = "Выполняет вход пользователя в систему. Поддерживает куки access_token и refresh_token. "
                           + "Если переданы валидные куки, может выполнить автоматический вход.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешная авторизация", 
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Неверные учетные данные")
    })
    public ResponseEntity<LoginResponseDTO> login(
        @Parameter(description = "Access token из куки (необязательный)", required = false)
        @CookieValue(name = "access_token", required = false) String access,
        
        @Parameter(description = "Refresh token из куки (необязательный)", required = false)
        @CookieValue(name = "refresh_token", required = false) String refresh,
        
        @Parameter(description = "Данные для входа", required = true)
        @RequestBody LoginRequestDTO request) {
        return authenticationService.login(request, access, refresh);
    }
    
    @PostMapping("/refresh")
    @Operation(summary = "Обновление токена доступа", 
               description = "Обновляет access token с использованием refresh token. "
                           + "Refresh token должен быть валидным и не истекшим.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Токен успешно обновлен", 
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Неверный или истекший refresh token"),
        @ApiResponse(responseCode = "400", description = "Refresh token не предоставлен")
    })
    public ResponseEntity<LoginResponseDTO> refresh(
        @Parameter(description = "Access token из куки (необязательный)", required = false)
        @CookieValue(name = "access_token", required = false) String access,
        
        @Parameter(description = "Refresh token из куки (обязательный для обновления)", required = false)
        @CookieValue(name = "refresh_token", required = false) String refresh){
        return authenticationService.refresh(refresh);
    }

    @PostMapping("/logout")
    @Operation(summary = "Выход из системы", 
               description = "Завершает сессию пользователя, инвалидирует токены.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Успешный выход из системы", 
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Неверные параметры запроса")
    })
    public ResponseEntity<LoginResponseDTO> logout(
        @Parameter(description = "Access token из куки (необязательный)", required = false)
        @CookieValue(name = "access_token", required = false) String access,
        
        @Parameter(description = "Refresh token из куки (необязательный)", required = false)
        @CookieValue(name = "refresh_token", required = false) String refresh){
        return authenticationService.logout(access, refresh);
    }

    @GetMapping("/info")
    @Operation(summary = "Получение информации о текущем пользователе", 
               description = "Возвращает данные текущего аутентифицированного пользователя.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Информация успешно получена", 
                    content = @Content(schema = @Schema(implementation = VMUserLoggedDTO.class))),
        @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован")
    })
    public ResponseEntity<VMUserLoggedDTO> info(){
        return ResponseEntity.ok(authenticationService.info());
    }

    @PostMapping("/reset")
    @Operation(summary = "Сброс пароля", 
               description = "Позволяет аутентифицированному пользователю изменить свой пароль. "
                           + "Требует предоставления старого пароля для подтверждения.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пароль успешно изменен", 
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Неверный старый пароль или пользователь не аутентифицирован")
    })
    public ResponseEntity<LoginResponseDTO> resetPassword(
        @Parameter(description = "Access token из куки (необязательный)", required = false)
        @CookieValue(name = "access_token", required = false) String access,
        
        @Parameter(description = "Refresh token из куки (необязательный)", required = false)
        @CookieValue(name = "refresh_token", required = false) String refresh,
        
        @Parameter(description = "Данные для сброса пароля", required = true)
        @RequestBody ResetPasswordDTO request){
        return authenticationService.resetPassword(request, access, refresh);
    }
    
    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя", 
               description = "Создает новую учетную запись пользователя в системе.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь успешно зарегистрирован", 
                    content = @Content(schema = @Schema(implementation = LoginResponseDTO.class)))
    })
    public ResponseEntity<LoginResponseDTO> register(
        @Parameter(description = "Данные для регистрации нового пользователя", required = true)
        @RequestBody RegisterRequestDTO request) {
        return authenticationService.register(request);
    }
}