package com.example.vmserver.util;

import java.io.Console;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    @Value("${cookie.secure:false}")
    private boolean secure;
    
    @Value("${cookie.same-site:Lax}")
    private String sameSite;

    @Value("${jwt.access.cookie-name}")
    private String accessTokenCookieName;

    @Value("${jwt.refresh.cookie-name}")
    private String refreshTokenCookieName;

    public HttpCookie createAccessCookie(String value, long duration){
        try {
            return ResponseCookie.from(accessTokenCookieName, value).maxAge(duration).httpOnly(true).secure(secure).path("/").sameSite(sameSite).build();
        }
        catch (Exception e) {
            return null;
        }
    }

    public HttpCookie deleteAccessCookie(){
        return ResponseCookie.from(accessTokenCookieName, "").maxAge(0).httpOnly(true).secure(secure).path("/").sameSite(sameSite).build();
    }

    public HttpCookie createRefreshCookie(String value, long duration){
        return ResponseCookie.from(refreshTokenCookieName, value).maxAge(duration).httpOnly(true).secure(secure).path("/").sameSite(sameSite).build();
    }

    public HttpCookie deleteRefreshCookie(){
        return ResponseCookie.from(refreshTokenCookieName, "").maxAge(0).httpOnly(true).secure(secure).path("/").sameSite(sameSite).build();
    }
}
