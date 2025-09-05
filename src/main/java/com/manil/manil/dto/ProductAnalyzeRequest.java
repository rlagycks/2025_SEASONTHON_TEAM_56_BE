package com.manil.manil.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class ProductAnalyzeRequest {

    @JsonProperty("name")
    @Size(max = 200, message = "상품명은 최대 200자입니다.")
    private String name; // nullable

    @JsonProperty("simple_description")
    @Size(max = 1000, message = "간단한 설명은 최대 1000자입니다.")
    private String simpleDescription; // nullable

    @JsonProperty("keywords")
    @Size(max = 5, message = "키워드는 최대 5개까지 입력 가능합니다.")
    private List<@Size(min = 1, max = 50, message = "키워드는 1~50자입니다.") String> keywords; // 0~5, nullable

    @JsonProperty("category")
    @Size(max = 100, message = "카테고리는 최대 100자입니다.")
    private String category; // nullable

    @JsonProperty("price")
    @DecimalMin(value = "0.0", inclusive = true, message = "가격은 0 이상이어야 합니다.")
    @Digits(integer = 13, fraction = 2, message = "가격 형식이 올바르지 않습니다.")
    private BigDecimal price; // nullable

    @JsonProperty("images")
    @Size(max = 5, message = "상품 이미지는 최대 5개까지 업로드 가능합니다.")
    private List<@Size(max = 500, message = "이미지 URL은 최대 500자입니다.") String> images; // 0~5, nullable
}
