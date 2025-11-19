package com.sparta.payment_system.security;

import com.sparta.payment_system.repository.BlacklistRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;
    private BlacklistRepository blacklistRepository;

    public JwtUtil(@Value("${jwt.secret.key}") String secretKey,
                   @Value("${jwt.token-validity-in-seconds}") long expiration,
                   BlacklistRepository blacklistRepository) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.accessExpiration = expiration * 1000; //밀리초 변환 - 1시간
        this.refreshExpiration = expiration * 24 * 7 * 1000; //밀리초 변환 - 7일

        this.blacklistRepository = blacklistRepository;
    }

    /**
     * Authentication - JWT Token 생성
     */
    public String createJwtToken(Authentication authentication, long expiration) {
        //권한 얻기
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        //만료기한
        Date expirationDate = new Date(System.currentTimeMillis() + expiration);
        //사용자 정보 꺼내기
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("auth", roles)
                .signWith(key, SignatureAlgorithm.HS256)
                .setExpiration(expirationDate)
                .compact();
    }

    /**
     * Access / Refresh Token 생성 메서드 분리
     */
    public String createAccessToken(Authentication authentication) {
        return createJwtToken(authentication, accessExpiration);
    }

    public String createRefreshToken(Authentication authentication) {
        return createJwtToken(authentication, refreshExpiration);
    }

    /**
     * JWT 토큰에서 인증 정보를 추출합니다.
     * <p>
     * Controller에서 (@AuthenticationPrincipal CustomUserDetails user)와 같이 인증된 사용자 정보를
     * 가져와 활용하시면 됩니다.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth", String.class).split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        org.springframework.security.core.userdetails.User principal =
                new User(claims.getSubject(), "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * JWT 토큰의 유효성을 검증합니다.
     * Access Token Refresh Token 검증 과정 분리
     */
    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            System.out.println("유효하지 않은 Token: {}" + e.getMessage());
            return false;
        }
    }
    public boolean validateRefreshToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);

            if (claims.getPayload().getExpiration().before(new Date())) {
                return false;
            }
            if (blacklistRepository.existsByToken(token)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("유효하지 않은 Token: {}" + e.getMessage());
            return false;
        }
    }
}
