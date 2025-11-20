package com.sparta.payment_system.dto.product;

import com.sparta.payment_system.entity.Product;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ProductCreateResponseDto {

    private final Long productId; // 상품아이디
    private final String name; // 상품명
    private final String description; // 상품 설명
    private final BigDecimal price; // 상품 가격
    private final Integer stock; // 재고
    private final LocalDateTime createdDate; // 상품 생성일

    public ProductCreateResponseDto(Long productId, String name, String description, BigDecimal price, Integer stock, LocalDateTime createdDate) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdDate = createdDate;
    }

    public static ProductCreateResponseDto from(Product product)
    {
        return new ProductCreateResponseDto(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCreatedAt()
        );
    }
}
