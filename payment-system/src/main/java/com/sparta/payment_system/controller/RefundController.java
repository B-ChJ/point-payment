package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.refund.RefundRequestDto;
import com.sparta.payment_system.dto.refund.RefundResponseDto;
import com.sparta.payment_system.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/refunds")
@CrossOrigin(origins = "*")
public class RefundController {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;

    @Autowired
    public RefundController(RefundRepository refundRepository, PaymentRepository paymentRepository, PaymentService paymentService) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
    }

    // 환불 요청 API
    @PostMapping("/request")
    public Mono<ResponseEntity<String>> requestRefund(@RequestBody RefundRequestDto refundRequest) {
        try {
            System.out.println("환불 요청 받음: " + refundRequest);

            // 1. 결제 정보 조회 및 검증
            Optional<Payment> paymentOptional = paymentRepository.findById(refundRequest.getPaymentId());
            if (paymentOptional.isEmpty()) {
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("결제 정보를 찾을 수 없습니다. Payment ID: " + refundRequest.getPaymentId()));
            }

            Payment payment = paymentOptional.get();

            // 2. 환불 가능 상태 확인
            if (payment.getStatus() != Payment.PaymentStatus.PAID){
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("환불할 수 없는 결제 상태입니다. 현재 상태: " + payment.getStatus()));
            }

            // 3. 환불 금액 설정 (전체 환불)
            final BigDecimal refundAmount = payment.getAmount();

            // 4. PortOne API로 환불 요청
            final String reason = refundRequest.getReason() != null ? refundRequest.getReason() : "사용자 요청에 의한 환불";

            return paymentService.cancelPayment(payment.getPaymentKey(), reason)
                    .map(isSuccess -> {
                        if (isSuccess) {
                            try {
                                Refund refund = new Refund();
                                refund.setPaymentId(payment.getPaymentId());
                                refund.setAmount(refundAmount);
                                refund.setReason(reason);
                                refund.setStatus(Refund.RefundStatus.COMPLETED);

                                refundRepository.save(refund);

                                // 결제 상태 업데이트 (부분 환불 제거)
                                payment.setStatus(Payment.PaymentStatus.REFUNDED);
                                paymentRepository.save(payment);

                                System.out.println("환불 처리 완료 - Payment ID: " + payment.getPaymentId() +
                                        ", Refund Amount: " + refundAmount);

                                return ResponseEntity.ok("환불이 성공적으로 처리되었습니다. 환불 금액: " + refundAmount);
                            } catch (Exception e) {
                                System.err.println("환불 DB 저장 중 오류: " + e.getMessage());
                                e.printStackTrace();
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body("환불은 성공했으나 DB 저장 중 오류가 발생했습니다: " + e.getMessage());
                            }
                        } else {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                    .body("PortOne 환불 요청이 실패했습니다.");
                        }
                    })
                    .onErrorReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("환불 처리 중 오류가 발생했습니다."));

        } catch (Exception e) {
            System.err.println("환불 요청 처리 중 오류: " + e.getMessage());
            e.printStackTrace();
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("환불 요청 처리 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

