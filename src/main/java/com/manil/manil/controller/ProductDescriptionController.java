package com.manil.manil.controller;

import com.manil.manil.dto.ProductAnalyzeRequest;
import com.manil.manil.dto.ProductAnalyzeResponse;
import com.manil.manil.global.payload.ResponseDto;
import com.manil.manil.service.GeminiProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductDescriptionController {

    private final GeminiProductService geminiProductService;

    /**
     * POST /api/products/analyze
     * - 입력: name, simple_description, keywords(0~5), category, price, images(0~5)
     * - 처리: 이미지 제외, 나머지 필드만 프롬프트에 반영하여 상세설명 생성
     * - 반환: ResponseDto<{ detailed_description, analyze_id }>
     */
    @PostMapping(
            path = "/analyze",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseDto<ProductAnalyzeResponse> analyze(@Valid @RequestBody ProductAnalyzeRequest request) {
        ProductAnalyzeResponse res = geminiProductService.generateDetailedDescription(request);
        // 판매자 수정 기능은 프론트에서 res.detailed_description을 편집하여 /products 저장 시 사용
        return ResponseDto.of(res);
    }
}
