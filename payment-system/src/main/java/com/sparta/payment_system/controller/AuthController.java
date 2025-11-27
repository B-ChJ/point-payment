package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.auth.*;
import com.sparta.payment_system.service.AuthService;
import com.sparta.payment_system.service.BlacklistService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final BlacklistService blacklistService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto request) {
        RegisterResponseDto result = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request,
                                                  HttpServletResponse response) {
        TokenResponseDto result = authService.login(request);

        String accessToken = result.getAccessToken();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", result.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        response.setHeader(HttpHeaders.AUTHORIZATION, accessToken);
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(new LoginResponseDto(accessToken, result.getUser()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDto> refresh(HttpServletRequest request,
                                                      HttpServletResponse response) {
        String refreshToken = getTokenFromCookie(request);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }

        RefreshResponseDto result = authService.refresh(refreshToken);
        if (result == null) { return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); }
        response.setHeader(HttpHeaders.AUTHORIZATION, result.getToken());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDto> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getTokenFromCookie(request);

        LogoutResponseDto result = blacklistService.addLogoutToken(refreshToken);
        // Refresh Token 쿠키 삭제
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return ResponseEntity.ok(result);
    }

    private String getTokenFromCookie(HttpServletRequest request) {
        String refreshToken = null;
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals("refreshToken")) {
                refreshToken = cookie.getValue();
            }
        }
        return refreshToken;
    }
}