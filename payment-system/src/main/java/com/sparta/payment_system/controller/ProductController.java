package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.product.ProductCreateRequestDto;
import com.sparta.payment_system.dto.product.ProductCreateResponseDto;
import com.sparta.payment_system.dto.product.ProductDetailResponseDto;
import com.sparta.payment_system.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductCreateRequestDto requestDto) {
        ProductCreateResponseDto responseDto = productService.createProduct(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponseDto> getProduct(@PathVariable Long productId) {
        ProductDetailResponseDto responseDto = productService.getProduct(productId);
        return ResponseEntity.ok(responseDto);
    }
}
