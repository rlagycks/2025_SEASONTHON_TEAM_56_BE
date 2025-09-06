// src/main/java/com/manil/manil/search/dto/SearchResponse.java
package com.manil.manil.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record SearchResponse(
        List<ProductHit> products
) {
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProductHit(
            Long id,
            String name,
            String description,   // simple_description 요약 등
            BigDecimal price,
            String category,
            Double similarity,
            @JsonProperty("main_image_url") String mainImageUrl  // ← 추가
    ) { }
}