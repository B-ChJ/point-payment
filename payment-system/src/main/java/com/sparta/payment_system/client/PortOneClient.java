package com.sparta.payment_system.client;

import com.sparta.payment_system.dto.payment.PaymentVerificationDto; // DTO 임포트
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

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
    public String getAccessToken() {
        return webClient.post()
                .uri("/login/api-secret")
                .bodyValue(Map.of("apiSecret", apiSecret))
                .retrieve()
                .onStatus(statusCode->statusCode.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println(" PortOne Payment Details Error Status: " + clientResponse.statusCode());
                                    System.err.println(" PortOne Payment Details Error Body: " + errorBody);
                                    return Mono.error(new RuntimeException("PortOne 결제 정보 조회 실패: " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .map(response -> (String) response.get("accessToken"))
                .blockOptional()
                .orElseThrow(() -> new RuntimeException("PortOne Acess Token 발급 실패"));
    }

    //  결제 ID로 결제 정보 조회
    public Map getPaymentDetails(String paymentId, String accessToken) {

        return webClient.get()
                .uri("/payments/{paymentId}", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .onStatus(statusCode->statusCode.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    System.err.println(" PortOne Payment Details Error Status: " + clientResponse.statusCode());
                                    System.err.println(" PortOne Payment Details Error Body: " + errorBody);
                                    return Mono.error(new RuntimeException("PortOne 결제 정보 조회 실패: " + errorBody));
                                })
                )
                .bodyToMono(Map.class)
                .blockOptional()
                .orElseThrow(() -> new IllegalArgumentException("PortOne에서 해당 paymentId의 결제정보를 찾을수 없습니다"));
    }

    // 결제 취소
    public Map cancelPayment(String paymentId, String accessToken, String reason) {

        return webClient.post()
                .uri("/payments/{paymentId}/cancel", paymentId)
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of("reason", reason))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }


    public PaymentVerificationDto getPayment(String paymentKey) {
        //  AccessToken 발급
        String accessToken = getAccessToken();


        Map details = getPaymentDetails(paymentKey, accessToken);

        return PaymentVerificationDto.builder()
                .paymentKey(paymentKey)
                .amount(new BigDecimal(details.get("amount").toString()))
                .status((String) details.get("status"))
                .orderId(Optional.ofNullable((String) details.get("merchant_uid")) .orElse(""))
                .build();
    }
}