package com.example.vmserver.jwt;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter{

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access.cookie-name}")
    private String accessCookieName;

    private final UserDetailsService userDetailsService;

    //Cписок endpoints, которые не требуют аутентификации
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth/login") ||
               requestURI.startsWith("/api/auth/refresh") ||
               requestURI.startsWith("/api/auth/register") ||
               requestURI.startsWith("/swagger-ui/") ||
               requestURI.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException{

        String requestURI = request.getRequestURI();
        
        // Если endpoint публичный - пропускаем без проверки JWT
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        Cookie[] cookies = request.getCookies();
        String token = "";
        
        if(cookies != null){
            for(Cookie cookie: cookies){
            if(accessCookieName.equals(cookie.getName())){
                token = cookie.getValue();
                break;
            }
            }
        }

        if("".equals(token) || !jwtTokenProvider.isValid(token)){
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtTokenProvider.getVMUserName(token);
        if(username == null){
            filterChain.doFilter(request, response);
            return;
        }
        
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
        filterChain.doFilter(request, response);
    }
}
