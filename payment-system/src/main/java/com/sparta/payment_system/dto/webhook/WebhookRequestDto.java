package com.sparta.payment_system.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class WebhookRequestDto {

    // PortOne(아임포트)에서 보내주는 결제 고유 ID (imp_uid)

    @JsonProperty("imp_uid")
    private String paymentKey;

    // 주문 번호
    @JsonProperty("merchant_uid")
    private String orderId;

    // 결제 상태
    private String status;
}


