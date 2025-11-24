package com.sparta.payment_system.client;

import com.sparta.payment_system.dto.payment.PaymentVerificationDto; // DTO 임포트
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class PortOneClient {

    private final WebClient webClient;
    private final String apiSecret;

    public PortOneClient(@Value("${portone.api.url}") String apiUrl,
                         @Value("${portone.api.secret}") String apiSecret) {
        this.webClient = WebClient.create(apiUrl);
        this.apiSecret = apiSecret;
    }

    // API Secret으로 인증 토큰 요청
    public Mono<String> getAccessToken() {
        // ... (기존 코드) ...
        return webClient.post()
                .uri("/login/api-secret")
                .bodyValue(Map.of("apiSecret", apiSecret))
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("accessToken"));
    }

    //  결제 ID로 결제 정보 조회
    public Mono<Map> getPaymentDetails(String paymentId, String accessToken) {
        // ... (기존 코드) ...
        return webClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // 결제 취소
    public Mono<Map> cancelPayment(String paymentId, String accessToken, String reason) {
        // ... (기존 코드) ...
        return webClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of("reason", reason))
                .retrieve()
                .bodyToMono(Map.class);
    }


    public PaymentVerificationDto getPayment(String paymentKey) {
        //  AccessToken 발급
        String accessToken = getAccessToken()
                .block(); // Mono의 결과를 기다림

        if (accessToken == null) {
            throw new RuntimeException("PortOne Access Token 발급 실패");
        }

        Map details = getPaymentDetails(paymentKey, accessToken)
                .block(); // Mono의 결과를 기다림

        if (details == null) {
            throw new IllegalArgumentException("PortOne에서 해당 paymentKey의 결제 정보를 찾을 수 없습니다.");
        }

        return PaymentVerificationDto.builder()
                .paymentKey(paymentKey)
                .amount(new BigDecimal(details.get("amount").toString()))
                .status((String) details.get("status"))
                .orderId((String) details.get("merchant_uid"))
                .build();
    }
}