package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.refund.RefundRequestDto;
import com.sparta.payment_system.dto.refund.RefundResponseDto;
import com.sparta.payment_system.security.CustomUserDetails;
import com.sparta.payment_system.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 환불(Refund) 관련 HTTP 요청을 처리하는 컨트롤러입니다.
 * 사용자 요청에 의한 환불을 처리합니다.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    /**
     * 특정 결제 건에 대해 환불을 요청합니다.
     * 인증된 사용자의 권한을 확인하고 PG사 환불 및 내부 시스템 후처리를 진행합니다.
     */
    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<RefundResponseDto> createRefund(
            @PathVariable Long paymentId,
            @RequestBody RefundRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 경로 변수로 받은 paymentId를 DTO에 설정합니다.
        requestDto.setPaymentId(paymentId);

        // 현재 로그인된 사용자 ID 추출 (Authentication.getName()이 일반적으로 principal의 ID를 반환한다고 가정)
        Long currentUserId = userDetails.getId();

        // RefundService 호출
        RefundResponseDto responseDto = refundService.createRefund(requestDto, currentUserId);
        return ResponseEntity.ok(responseDto);
    }
}
