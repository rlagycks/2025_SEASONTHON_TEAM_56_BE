package com.manil.manil.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.manil.manil.dto.ProductDescriptionRequest;
import com.manil.manil.dto.ProductDescriptionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiProductService {

    private final Client client;

    // 기본 모델: 2.5-flash
    @Value("${gemini.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${gemini.max-output-tokens:512}")
    private Integer maxOutputTokens;

    public GeminiProductService(Client client) {
        this.client = client;
    }

    public ProductDescriptionResponse generate(ProductDescriptionRequest req) {
        String sysPrompt = buildSystemPrompt(req);

        GenerateContentConfig config = GenerateContentConfig.builder()
                .maxOutputTokens(maxOutputTokens)
                .build();

        // 권장 사용법: client.models.generateContent(model, prompt, config)
        GenerateContentResponse response =
                client.models.generateContent(modelName, sysPrompt, config);

        String text = (response != null && response.text() != null && !response.text().isBlank())
                ? response.text().trim()
                : "설명을 생성하지 못했습니다. 입력을 확인하거나 다시 시도해주세요.";

        return new ProductDescriptionResponse(text, modelName, maxOutputTokens);
    }

    private String buildSystemPrompt(ProductDescriptionRequest req) {
        String locale = (req.getLocale() == null || req.getLocale().isBlank()) ? "ko" : req.getLocale();
        String tone = (req.getTone() == null || req.getTone().isBlank()) ? "중립" : req.getTone();
        String keywords = (req.getKeywords() == null) ? "" : req.getKeywords();

        return """
                역할: 전자상거래 카피라이터
                목표: 한 줄 설명을 바탕으로 상세 상품설명을 생성한다.

                출력 언어: %s
                톤: %s
                분량: 250~400자, 3~6문장

                작성 원칙:
                1) 핵심 특징(소재/기능/호환/사이즈 등), 효용, 사용 시나리오를 포함.
                2) 확정되지 않은 정보는 "~일 수 있습니다", "~로 보입니다" 등의 책임 있는 어투 사용.
                3) 과장/과도한 마케팅 표현/이모지/특수기호 금지. 가격·재고·보증 등 미확정 정보 단정 금지.
                4) 헤더/목록 없이 자연스러운 단일 문단.
               

                추가 힌트(선택): %s

                한 줄 설명:
                "%s"

                위 조건을 모두 지켜 상세설명을 작성하세요.
                """.formatted(locale, tone, keywords, req.getOneLine());
    }
}
