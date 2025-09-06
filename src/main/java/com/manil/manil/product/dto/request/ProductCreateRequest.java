package com.manil.manil.product.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductCreateRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @JsonProperty("simple_description")              // 기본 키 이름(응답 시에도 이 이름으로 나감)
    @JsonAlias({"simpleDescription"})                // 요청에서 카멜케이스도 허용
    @Size(max = 2000, message = "간단 설명은 2000자 이하이어야 합니다.")
    private String simpleDescription;

    @JsonProperty("detailed_description")
    @JsonAlias({"detailedDescription"})
    @NotBlank(message = "상세 설명은 필수입니다.")
    @Size(max = 10000, message = "상세 설명은 10000자 이하이어야 합니다.")
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
}