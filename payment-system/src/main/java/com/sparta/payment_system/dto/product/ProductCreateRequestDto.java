package com.sparta.payment_system.dto.product;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ProductCreateRequestDto {

    @NotBlank(message = "상품명은 필수 입력 항목입니다.")
    @Size(max = 255, message = "상품명은 255자를 초과할 수 없습니다.")
    private String name;

    @NotBlank(message = "상품 설명은 필수 입력 항목입니다.")
    @Size(max = 5000,message = "상품 설명은 5000자를 초과할 수 없습니다.")
    private String description;

    @NotNull(message = "상품 가격은 필수 입력 항목입니다.")
    private BigDecimal price;

    @NotNull(message = "재고 수량은 필수 입력 항목입니다.")
    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다.")
    private Integer stock;
}
