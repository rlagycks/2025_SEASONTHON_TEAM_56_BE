// src/main/java/com/manil/manil/search/repository/projection/SearchRow.java
package com.manil.manil.search.repository;

import java.math.BigDecimal;

public interface SearchRow {
    Long getId();
    String getName();
    String getDetailedDescription();
    BigDecimal getPrice();
    String getCategory();
    Double getSimilarity();
}