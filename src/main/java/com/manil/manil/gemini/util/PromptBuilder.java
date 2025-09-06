package com.manil.manil.gemini.util;

import com.manil.manil.gemini.client.LlmAnalyzeClient;

// gemini/util/PromptBuilder.java
public final class PromptBuilder {
    private PromptBuilder() {}
    public static String systemPrompt() {
        return """
        역할: 전자상거래 카피라이터 & 상품 메타데이터 추출기
        - 입력을 바탕으로 상세설명/filters/abstract_tags 산출
        - 반드시 JSON만 반환 (application/json)
        - 스키마:
          {
            "detailed_description": "string",
            "ingredients": ["string"],
            "regions": ["string"],
            "age_groups": ["string"],
            "abstract_tags": ["string"]
          }
        - 250~400자, 3~6문장, 과장/이모지/특수기호 금지, 구매결정에 유용한 구체성 포함
        - 추정은 단정 금지, '~일 수 있습니다' 등 책임 있는 어투
        """;
    }
    public static String userPrompt(LlmAnalyzeClient.AnalyzeInput in) {
        var sb = new StringBuilder("[입력]\n");
        if (in.name() != null) sb.append("name: ").append(in.name()).append('\n');
        if (in.simpleDescription() != null) sb.append("simple_description: ").append(in.simpleDescription()).append('\n');
        if (in.keywords() != null && !in.keywords().isEmpty())
            sb.append("keywords: ").append(String.join(", ", in.keywords())).append('\n');
        if (in.category() != null) sb.append("category: ").append(in.category()).append('\n');
        if (in.price() != null) sb.append("price: ").append(in.price()).append('\n');
        sb.append("\n[출력] 스키마에 맞는 JSON만 반환\n");
        return sb.toString();
    }
}