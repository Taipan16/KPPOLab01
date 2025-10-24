package com.example.vmserver.jwt;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.example.vmserver.enums.TokenType;
import com.example.vmserver.model.Token;
import com.example.vmserver.repository.TokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;



@Service
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String key;

    private final TokenRepository tokenRepository;

    public JwtTokenProvider(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    private boolean isDisabled(String value){
        Token token = tokenRepository.findByValue(value).orElse(null);

        if(token == null){
            return false;
        }
        return token.isDisabled();
    }

    private Date toDate(LocalDateTime time){
        return Date.from(time.toInstant(ZoneOffset.UTC));

    }

    private LocalDateTime toLocalDateTime(Date time){
        return time.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    private Claims extractAllClaims(String value){
        return Jwts.parser.Builder().setSigningKey(decodeSecretKey(key)).build().parseClaimsJwt(value).getBody();
    }

    private Key decodeSecretKey(String key){
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(key));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver){
        return claimsResolver.apply(extractAllClaims(token));
    }

    public String getVMUserName(String token){
        return extractClaim(token, Claims::getSubject);
    }

    public LocalDateTime getExpiration(String token){
        return toLocalDateTime(extractClaim(token, Claims::getExpiration));
    }

    public boolean isValid(String token){
        if(token == null){
            return false;
        }
        try{
            Jwts.parserBuilder().setSingingKey(decodeSecretKey(key)).build().parseClaimsJwt(token);
            return !isDisabled(token);
        }
        catch(JwtException e){
            return false;
        }
    }

    public Token generateAccessToken (Map <String, Object> extra, long duration, TemporalUnit durationType, UserDetails user){
        String username = user.getUsername();

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime expirationDate = now.plus(duration, durationType);

        String value = Jwts.builder().setSubject(username)
        .setIssuedAt(toDate(now))
        .setExpiration(toDate(expirationDate))
        .signWith(decodeSecretKey(key), SignatureAlgorithm.HS256).compact();

        return new Token(TokenType.REFRESH, value, expirationDate, false, null);
    }
}
