package com.sparta.payment_system.security;

import com.sparta.payment_system.entity.User;
import com.sparta.payment_system.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        //사용자 정보를 UserDetails 객체로 변환
        return new CustomUserDetails(user);
    }
}