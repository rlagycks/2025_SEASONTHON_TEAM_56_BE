// AnalyzeRequest.java
package com.manil.manil.product.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyzeRequest {
    private String name;

    @JsonProperty("simple_description")
    private String simpleDescription;

    private List<String> keywords;
    private String category;
    private BigDecimal price; // 모두 nullable
}