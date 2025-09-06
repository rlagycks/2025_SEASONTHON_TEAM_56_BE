package com.manil.manil.product.dto.common;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record AnalyzeCacheUpsert(
        String analyzeId,                   // UUID 문자열
        String requestName,
        String requestSimpleDescription,
        String requestCategory,
        BigDecimal requestPrice,
        List<String> requestKeywords,

        String detailedDescription,         // LLM 생성 상세설명
        float[] embedding,                  // LLM 생성 임베딩
        List<String> ingredients,
        List<String> regions,
        List<String> ageGroups,
        List<String> abstractTags
) { }