package com.manil.manil.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProductAnalyzeResponse {

    @JsonProperty("detailed_description")
    private String detailedDescription;

    @JsonProperty("analyze_id")
    private String analyzeId;
}
