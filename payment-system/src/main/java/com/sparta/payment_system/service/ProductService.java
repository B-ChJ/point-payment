package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.product.ProductCreateRequestDto;
import com.sparta.payment_system.dto.product.ProductCreateResponseDto;
import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductCreateResponseDto createProduct(ProductCreateRequestDto requestDto) {

        Product product = Product.builder()
                .name(requestDto.getName())
                .price(requestDto.getPrice())
                .stock(requestDto.getStock())
                .description(requestDto.getDescription())
                .build();

        productRepository.save(product);

        return ProductCreateResponseDto.from(product);
    }

    public ProductCreateResponseDto getProductInfo(Long id) {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new RuntimeException("존재하지 않는 상품입니다."));

        return ProductCreateResponseDto.from(product);
    }
}
