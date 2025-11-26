package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.MyInfoResponseDto;
import com.sparta.payment_system.dto.PointBalanceResponseDto;
import com.sparta.payment_system.security.CustomUserDetails;
import com.sparta.payment_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

//    @Autowired
//    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    /**
     * 내 정보 조회 API
     * @return User 객체
     */
    @GetMapping("/users/me")
    public ResponseEntity<MyInfoResponseDto> getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        Long userId = customUserDetails.getId();
        MyInfoResponseDto response = userService.getMyInfo(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 보유 포인트 조회 API
     */
    @GetMapping("/users/me/points")
    public ResponseEntity<PointBalanceResponseDto> getMyPoints(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Long userId = customUserDetails.getId();
        PointBalanceResponseDto response = userService.getMyPointBalance(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/points/charge/{userId}")
    public ResponseEntity<PointBalanceResponseDto> chargePoints(@PathVariable Long userId) {
        PointBalanceResponseDto result = userService.charge(userId);

        return ResponseEntity.ok(result);
    }

}