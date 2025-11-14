    package com.example.vmserver.service;


    import java.time.LocalDateTime;
    import java.time.temporal.ChronoUnit;
    import java.util.Map;
    import java.util.Set;

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
    import com.example.vmserver.dto.ResetPasswordDTO;
    import com.example.vmserver.dto.VMUserLoggedDTO;
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
            }

            if (!refreshValid || accessValid) {
                Token newRefresh = jwtTokenProvider.generatedRefreshToken(refreshDurationDate, ChronoUnit.DAYS, user);

                newRefresh.setVmUser(user);
                addRefreshTokenCookie(headers, newRefresh);
                tokenRepository.save(newRefresh);
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);

            return ResponseEntity.ok().headers(headers).body(new LoginResponseDTO(true, user.getRole().getName()));
        }

        @Override
        public ResponseEntity <LoginResponseDTO> refresh(String refreshToken)
        {
            if(!jwtTokenProvider.isValid(refreshToken)){
                throw new RuntimeException("token is invalid");
            }
            VMUser user = vmUserService.getVMUser(jwtTokenProvider.getVMUserName(refreshToken));

            Token newAccess = jwtTokenProvider.generatedAccessToken(Map.of("role", user.getRole().getAuthority()), accessDurationMin, ChronoUnit.MINUTES, user);

            newAccess.setVmUser(user);
            HttpHeaders headers = new HttpHeaders();
            addAccessTokenCookie(headers, newAccess);

            tokenRepository.save(newAccess);

            return ResponseEntity.ok().headers(headers).body(new LoginResponseDTO(true, user.getRole().getName()));
        }

        @Override
        public ResponseEntity <LoginResponseDTO> logout(String accessToken, String refresh){
            SecurityContextHolder.clearContext();
            VMUser user = vmUserService.getVMUser(jwtTokenProvider.getVMUserName(accessToken));
            revokeAllTokens(user);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, cookieUtil.deleteAccessCookie().toString());
            headers.add(HttpHeaders.SET_COOKIE, cookieUtil.deleteRefreshCookie().toString());

            return ResponseEntity.ok().headers(headers).body(new LoginResponseDTO(false, null));
        }
        
        @Override
        public VMUserLoggedDTO info(){
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if(authentication instanceof AnonymousAuthenticationToken){
                throw new RuntimeException("No user");
            }

            VMUser user = vmUserService.getVMUser(authentication.getName());

            return VMUserMapper.userToUserLoggedDto(user);
        }

        @Override
        public ResponseEntity<LoginResponseDTO> resetPassword(ResetPasswordDTO request, String access, String refresh){
            VMUser user = vmUserService.getVMUser(jwtTokenProvider.getVMUserName(access));
            
            vmUserService.resetPassword(user.getUsername(), request.oldPassword(), request.newPassord());
            return logout(access, refresh);
        }

    }
