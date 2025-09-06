// com.manil.manil.search.app.HardFilter.java
package com.manil.manil.search.filter;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record HardFilter(
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice
) { }