package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.MyInfoResponseDto;
import com.sparta.payment_system.dto.PointBalanceResponseDto;
import com.sparta.payment_system.entity.MembershipRank;
import com.sparta.payment_system.entity.User;
import com.sparta.payment_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 로그인한 사용자 정보 조회
     *
     * @param userId 로그인한 사용자 ID
     * @return 사용자 정보
     */
    @Transactional(readOnly = true)
    public MyInfoResponseDto getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. userId: " + userId));

        return new MyInfoResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getMembershipRank().name()
        );
    }

    @Transactional(readOnly = true)
    public PointBalanceResponseDto getMyPointBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. userId: " + userId));

        return new PointBalanceResponseDto(
                user.getUserId(),
                user.getEmail(),
                user.getTotalPoints()
        );
    }
}
