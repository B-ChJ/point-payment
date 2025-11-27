package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.payment.PaymentRequestDto;
import com.sparta.payment_system.dto.payment.PaymentResponseDto;
import com.sparta.payment_system.service.PaymentService;
import com.sparta.payment_system.dto.payment.PortOnePaymentReadyResponseDto;
import com.sparta.payment_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 결제(Payment) 관련 HTTP 요청을 처리하는 컨트롤러입니다.
 * 결제 준비, 완료 처리 및 결제 내역 조회를 담당합니다.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;


    /**
     * 특정 주문에 대해 결제 준비를 요청합니다.
     * PortOne 연동에 필요한 Payment Key 및 최종 결제 금액을 반환합니다.
     */
    @PostMapping("/{orderId}/payments")
    public ResponseEntity<PortOnePaymentReadyResponseDto> createPayment(
            @PathVariable Long orderId,
            @RequestBody PaymentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long currentUserId = userDetails.getId();

        PortOnePaymentReadyResponseDto dto = paymentService.createPaymentReady(
                currentUserId,
                orderId,
                requestDto
        );

        return ResponseEntity.ok(dto);
    }

    /**
     * PortOne 결제창을 통해 결제가 성공적으로 이루어진 후, 최종 검증 및 후처리를 수행합니다.
     */
    @PostMapping("/complete")
    public ResponseEntity<PaymentResponseDto> completePayment(
            @RequestParam String paymentKey,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {


        Long currentUserId = userDetails.getId();

        PaymentResponseDto dto = paymentService.completePaymentVerification(
                paymentKey,
                currentUserId
        );
        return ResponseEntity.ok(dto);
    }


    /**
     * 특정 결제 ID에 해당하는 상세 결제 내역을 조회합니다.
     * 접근 권한(본인 여부)을 확인합니다.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getId();


        PaymentResponseDto dto = paymentService.getPaymentDetails(paymentId, currentUserId);
        return ResponseEntity.ok(dto);
    }
}