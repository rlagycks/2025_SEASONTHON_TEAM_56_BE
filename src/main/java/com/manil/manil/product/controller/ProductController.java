// com.manil.manil.product.api.ProductController.java
package com.manil.manil.product.controller;

import com.manil.manil.global.payload.ResponseDto;
import com.manil.manil.product.service.ProductService;
import com.manil.manil.product.dto.request.ProductCreateRequest;
import com.manil.manil.product.dto.response.ProductCreatedResponse;
import com.manil.manil.product.dto.response.ProductDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 상품 등록 */
    @PostMapping
    public ResponseEntity<ResponseDto<ProductCreatedResponse>> create(
            @Valid @RequestBody ProductCreateRequest req
    ) {
        Long id = productService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.of(
                        ProductCreatedResponse.builder().productId(id).build()
                ));
    }

    /** 상품 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<ProductDetailResponse>> detail(@PathVariable Long id) {
        var detail = productService.getDetail(id);
        return ResponseEntity.ok(ResponseDto.of(detail));
    }
}