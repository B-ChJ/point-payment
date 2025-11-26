package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.refund.RefundRequestDto;
import com.sparta.payment_system.dto.refund.RefundResponseDto;
import com.sparta.payment_system.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    // 환불 요청 API
    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<RefundResponseDto> createRefund(
            @RequestBody Long paymentId,
            @RequestBody RefundRequestDto requestDto,
            Authentication authentication
    ) {

        requestDto.setPaymentId(paymentId);

        // 현재 로그인된 사용자 ID 추출
        Long currentUserId = Long.parseLong(authentication.getName());

        // RefundService 호출
        RefundResponseDto responseDto = refundService.createRefund(requestDto, currentUserId);
        return ResponseEntity.ok(responseDto);
    }
}

