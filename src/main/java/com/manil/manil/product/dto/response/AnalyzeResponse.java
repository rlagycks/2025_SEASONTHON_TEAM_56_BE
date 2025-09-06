// src/main/java/com/manil/manil/product/dto/response/AnalyzeResponse.java
package com.manil.manil.product.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class AnalyzeResponse {
    @JsonProperty("detailed_description")
    private String detailedDescription;

    @JsonProperty("analyze_id")
    private String analyzeId;
}