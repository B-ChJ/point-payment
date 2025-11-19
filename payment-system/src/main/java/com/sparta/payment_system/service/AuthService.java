package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.auth.RegisterRequestDto;
import com.sparta.payment_system.dto.auth.LoginResponseDto;
import com.sparta.payment_system.dto.auth.RegisterResponseDto;
import com.sparta.payment_system.entity.User;
import com.sparta.payment_system.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }
        User user = new User(request.getEmail(),
                request.getPasswordHash(),
                request.getName());

        User savedUser = userRepository.save(user);

        return new RegisterResponseDto(savedUser);
    }

}
