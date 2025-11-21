package com.sparta.payment_system.dto.product;

import com.sparta.payment_system.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProductDetailResponseDto {

    private Long productId;
    private String name;
    private BigDecimal price;
    private int stock;
    private LocalDateTime createdAt;

    public static ProductDetailResponseDto from(Product product) {
        return new ProductDetailResponseDto(
                product.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getCreatedAt()
        );
    }
}
