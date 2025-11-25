package com.sparta.payment_system.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class LogoutResponseDto {
    private final String message;
    private final LocalDateTime loggedOutAt;

    public static LogoutResponseDto save(boolean success) {
        if(!success) {
            return new LogoutResponseDto("이미 로그아웃된 사용자입니다. 로그아웃 실패하였습니다.", LocalDateTime.now());
        }
        return new LogoutResponseDto("로그아웃 되었습니다.", LocalDateTime.now());
    }
}
