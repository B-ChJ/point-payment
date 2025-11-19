package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.auth.RegisterRequestDto;
import com.sparta.payment_system.dto.auth.LoginResponseDto;
import com.sparta.payment_system.dto.auth.RegisterResponseDto;
import com.sparta.payment_system.service.BlacklistService;
import com.sparta.payment_system.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final BlacklistService blacklistService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@RequestBody RegisterRequestDto request) {
        RegisterResponseDto result = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
