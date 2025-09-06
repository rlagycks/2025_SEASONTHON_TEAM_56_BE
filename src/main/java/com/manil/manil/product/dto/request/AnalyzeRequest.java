// src/main/java/com/manil/manil/product/dto/request/AnalyzeRequest.java
package com.manil.manil.product.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class AnalyzeRequest {
    private String name;

    @JsonProperty("simple_description")
    @JsonAlias({"simpleDescription"})
    private String simpleDescription;

    private List<String> keywords;

    private String category;

    private BigDecimal price;

    @JsonProperty("image_urls")
    private List<String> imageUrls; // JSON 모드에서만 사용(0번만 캐시 저장)
}