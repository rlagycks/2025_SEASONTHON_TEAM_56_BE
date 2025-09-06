// src/main/java/com/manil/manil/product/dto/request/ProductCreateRequest.java
package com.manil.manil.product.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
public class ProductCreateRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @JsonProperty("simple_description")
    @JsonAlias({"simpleDescription"})
    private String simpleDescription;

    @JsonProperty("detailed_description")
    @JsonAlias({"detailedDescription"})
    @NotBlank(message = "상세 설명은 필수입니다.")
    private String detailedDescription;

    @NotBlank(message = "카테고리는 필수입니다.")
    private String category;

    @NotNull(message = "가격은 필수입니다.")
    @PositiveOrZero(message = "가격은 0 이상이어야 합니다.")
    @Digits(integer = 10, fraction = 2, message = "가격 형식이 올바르지 않습니다.")
    private BigDecimal price;

    private List<@NotBlank(message = "키워드는 공백일 수 없습니다.") String> keywords;

    @JsonProperty("analyze_id")
    @JsonAlias({"analyzeId"})
    private String analyzeId; // nullable

    @JsonProperty("image_urls")
    private List<String> imageUrls; // 등록 시 전체 이미지 URL들(캐시/외부 혼합 가능)

    @JsonProperty("main_index")
    private Integer mainIndex; // 기본 0
}