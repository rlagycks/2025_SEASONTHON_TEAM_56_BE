// src/main/java/com/manil/manil/product/controller/ProductController.java
package com.manil.manil.product.controller;

import com.manil.manil.global.payload.ResponseDto;
import com.manil.manil.product.dto.request.ProductCreateRequest;
import com.manil.manil.product.dto.response.ProductCreatedResponse;
import com.manil.manil.product.dto.response.ProductDetailResponse;
import com.manil.manil.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** 상품 등록: multipart (payload JSON + images[]) */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<ProductCreatedResponse>> create(
            @Valid @RequestPart("payload") ProductCreateRequest req,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        Long id = productService.create(req, images);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.of(ProductCreatedResponse.builder().productId(id).build()));
    }

    /** 상품 상세 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<ProductDetailResponse>> detail(@PathVariable Long id) {
        var detail = productService.getDetail(id);
        return ResponseEntity.ok(ResponseDto.of(detail));
    }
}