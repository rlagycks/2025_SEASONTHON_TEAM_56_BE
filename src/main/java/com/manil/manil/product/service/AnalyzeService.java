// src/main/java/com/manil/manil/product/service/AnalyzeService.java
package com.manil.manil.product.service;

import com.manil.manil.product.dto.request.AnalyzeRequest;
import com.manil.manil.product.dto.response.AnalyzeResponse;
import com.manil.manil.image.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyzeService {

    private final ImageStorageService imageStorageService;
    // private final LlmClient llm; // 실제 LLM 클라이언트가 있다면 주입

    /** multipart OR json 두 경우 모두 여기로 모아 처리 */
    public AnalyzeResponse analyze(AnalyzeRequest req, List<MultipartFile> multipartImages, boolean isMultipart) {
        String analyzeId = UUID.randomUUID().toString();

        try {
            if (isMultipart) {
                imageStorageService.saveAnalyzeMainFromMultipart(multipartImages, analyzeId);
            } else {
                imageStorageService.saveAnalyzeMainFromUrl(req.getImageUrls(), analyzeId);
            }
        } catch (Exception e) {
            // 이미지 저장 실패는 분석 자체를 막진 않되, 필요 시 로깅/경고
        }

        // 상세 설명 생성 (여기선 가짜 로직; 실제론 LLM 호출)
        String detailed = generateDetailedDescription(req);

        return AnalyzeResponse.builder()
                .analyzeId(analyzeId)
                .detailedDescription(detailed)
                .build();
    }

    private String generateDetailedDescription(AnalyzeRequest req) {
        // TODO: 실제 LLM 호출/프롬프트 구성
        return (req.getName() != null ? req.getName() + "은(는) " : "") + "상세 설명 자동 생성 결과입니다.";
    }
}