// src/main/java/com/manil/manil/search/dto/SearchResponse.java
package com.manil.manil.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
            String description,  // simple_description 또는 상세 요약(서비스 규칙에 맞게)
            BigDecimal price,
            String category,
            Double similarity
    ) { }
}