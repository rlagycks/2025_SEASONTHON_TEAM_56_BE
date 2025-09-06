// AnalyzeResponse.java  (프론트 노출: 상세설명 + analyze_id 만)
package com.manil.manil.product.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record AnalyzeResponse(
        @JsonProperty("detailed_description") String detailedDescription,
        @JsonProperty("analyze_id") String analyzeId
) { }