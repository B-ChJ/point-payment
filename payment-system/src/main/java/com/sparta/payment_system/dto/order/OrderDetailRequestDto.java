package com.sparta.payment_system.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderDetailRequestDto {

    @NotEmpty(message = "상품 목록은 비어 있을 수 없습니다.")
    @Valid
    private List<ItemDto> productList;

    @Getter
    public static class ItemDto {
        @NotNull(message = "상품 ID는 필수입니다.")
        private Long productId;

        @NotNull(message = "수량은 필수입니다.")
        @Min(value = 1, message = "수량은 최소 1개 이상이어야 합니다.")
        private Integer quantity;
    }
}

