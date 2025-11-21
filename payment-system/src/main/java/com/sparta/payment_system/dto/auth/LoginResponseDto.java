package com.sparta.payment_system.dto.auth;

import com.sparta.payment_system.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private final String email;
    private final Long userId;
    private final String name;
    private final String membershipRank;

    public LoginResponseDto(User user) {
        this.email = user.getEmail();
        this.userId = user.getUserId();
        this.name = user.getName();
        this.membershipRank = user.getMembershipRank().name();
    }
}
