// com.manil.manil.ai.LlmAnalyzeClient.java
package com.manil.manil.gemini.client;

import java.util.List;

/** 상세설명 + 임베딩 + 필터 + 추상태그를 한 번에 뽑는 분석 클라이언트 */
public interface LlmAnalyzeClient {

    record AnalyzeInput(
            String name,
            String simpleDescription,
            List<String> keywords,
            String category,
            java.math.BigDecimal price
    ) {}

    record AnalyzeOutput(
            String detailedDescription,
            float[] embedding,
            List<String> ingredients,
            List<String> regions,
            List<String> ageGroups,
            List<String> abstractTags
    ) {}

    AnalyzeOutput extractAll(AnalyzeInput input);
}