package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.auth.LoginRequestDto;
import com.sparta.payment_system.dto.auth.RegisterRequestDto;
import com.sparta.payment_system.dto.auth.RegisterResponseDto;
import com.sparta.payment_system.dto.auth.TokenResponseDto;
import com.sparta.payment_system.entity.MembershipRank;
import com.sparta.payment_system.entity.User;
import com.sparta.payment_system.repository.UserRepository;
import com.sparta.payment_system.security.CustomUserDetails;
import com.sparta.payment_system.security.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User(request.getEmail(),
                passwordHash,
                request.getName());
        user.setMembershipRank(MembershipRank.NORMAL);

        User savedUser = userRepository.save(user);

        return new RegisterResponseDto(savedUser);
    }

    @Transactional
    public TokenResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(
                () -> new IllegalStateException("존재하지 않는 사용자입니다."));

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalStateException("입력값이 유효하지 않습니다.");
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails,
                "",
                userDetails.getAuthorities());

        String accessToken = jwtUtil.createAccessToken(authentication);
        String refreshToken = jwtUtil.createRefreshToken(authentication);

        return new TokenResponseDto(accessToken, refreshToken, user);

    }
}
