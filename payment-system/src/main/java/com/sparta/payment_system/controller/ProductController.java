package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductRepository productRepository;

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            System.out.println("상품 생성 요청 받음: " + product);
            Product savedProduct = productRepository.save(product);
            System.out.println("상품 저장 완료: " + savedProduct);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            System.err.println("상품 생성 에러: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("상품 생성 실패: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        try {
            Optional<Product> product = productRepository.findById(id);
            return product.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        try {
            if (productRepository.existsById(id)) {
                productRepository.deleteById(id);
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
