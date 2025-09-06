// src/main/java/com/manil/manil/product/dto/response/ProductDetailResponse.java
package com.manil.manil.product.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ProductDetailResponse(
        Long id,
        String name,
        @JsonProperty("simple_description") String simpleDescription,
        List<String> keywords,
        @JsonProperty("detailed_description") String detailedDescription,
        String category,
        BigDecimal price,
        List<ImageDto> images   // ← 추가
) {
    @Builder
    public record ImageDto(
            String url,
            @JsonProperty("is_main") boolean isMain,
            @JsonProperty("sort_order") int sortOrder
    ) {}
}