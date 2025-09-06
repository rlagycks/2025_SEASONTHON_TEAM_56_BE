// com.manil.manil.analyze.api.AnalyzeController.java
package com.manil.manil.product.controller;

import com.manil.manil.product.service.AnalyzeService;
import com.manil.manil.product.dto.request.AnalyzeRequest;
import com.manil.manil.product.dto.response.AnalyzeResponse;
import com.manil.manil.global.payload.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/products") // 최종 경로: /api/products/analyze
@RequiredArgsConstructor
public class AnalyzeController {

    private final AnalyzeService analyzeService;

    /**
     * AI 분석 호출
     * - 입력 값에서 null은 제거 후 프롬프트 구성
     * - 상세설명 + analyze_id만 프론트로 반환
     * - 임베딩/필터/추상태그는 서버(analyze_cache)에 저장
     */
    @PostMapping("/analyze")
    public ResponseEntity<ResponseDto<AnalyzeResponse>> analyze(@RequestBody AnalyzeRequest request) {
        var result = analyzeService.analyze(request);
        return ResponseEntity.ok(ResponseDto.of(result));
    }
}