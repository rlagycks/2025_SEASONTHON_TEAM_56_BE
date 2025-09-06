package com.manil.manil.product.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record ProductCreatedResponse(
        @JsonProperty("product_id") Long productId
) {}