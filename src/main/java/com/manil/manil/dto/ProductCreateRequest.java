package com.manil.manil.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class ProductCreateRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    @JsonProperty("name")
    private String name;

    @NotBlank(message = "간단한 설명은 필수입니다.")
    @JsonProperty("simple_description")
    private String simpleDescription;

    @Size(max = 5, message = "키워드는 최대 5개까지 입력 가능합니다.")
    @JsonProperty("keywords")
    private List<@Size(min = 1, max = 50, message = "키워드는 1~50자입니다.") String> keywords; // 이번 단계에선 저장 보류

    @NotBlank(message = "상세설명은 필수입니다.")
    @JsonProperty("detailed_description")
    private String detailedDescription;

    @NotBlank(message = "카테고리는 필수입니다.")
    @JsonProperty("category")
    private String category;

    @NotNull(message = "가격은 필수입니다.")
    @DecimalMin(value = "0.0", inclusive = true, message = "가격은 0 이상이어야 합니다.")
    @Digits(integer = 13, fraction = 2, message = "가격 형식이 올바르지 않습니다.")
    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("analyze_id")
    private String analyzeId; // 선택(분석 단계 임시 식별자)

    @Size(max = 5, message = "상품 이미지는 최대 5개까지 업로드 가능합니다.")
    @JsonProperty("images")
    private List<@Size(max = 500, message = "이미지 URL은 최대 500자입니다.") String> images; // 전시용, 저장 보류
}
