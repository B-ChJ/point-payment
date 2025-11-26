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

