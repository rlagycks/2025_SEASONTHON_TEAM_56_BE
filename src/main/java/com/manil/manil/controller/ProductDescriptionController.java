package com.manil.manil.controller;

import com.manil.manil.dto.ProductDescriptionRequest;
import com.manil.manil.dto.ProductDescriptionResponse;
import com.manil.manil.service.GeminiProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductDescriptionController {

    private final GeminiProductService geminiProductService;

    public ProductDescriptionController(GeminiProductService geminiProductService) {
        this.geminiProductService = geminiProductService;
    }

    /**
     * 예: POST /api/products/123/describe
     * body:
     * {
     *   "oneLine": "선물용 500ml 스테인리스 텀블러, 보온/보냉",
     *   "locale": "ko",
     *   "tone": "친절",
     *   "keywords": "선물, 스테인리스, 밀폐 뚜껑"
     * }
     */
    @PostMapping("/{productId}/describe")
    public ResponseEntity<ProductDescriptionResponse> describe(
            @PathVariable String productId,
            @Valid @RequestBody ProductDescriptionRequest request
    ) {
        // productId는 로깅/추후 프롬프트 보강용으로 활용 가능
        ProductDescriptionResponse res = geminiProductService.generate(request);
        return ResponseEntity.ok(res);
    }
}
