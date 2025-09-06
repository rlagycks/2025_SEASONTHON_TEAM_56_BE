// com.manil.manil.search.app.HardFilterParser.java
package com.manil.manil.search.filter;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HardFilterParser {
    private HardFilterParser() {}

    private static final Pattern CAT = Pattern.compile("(?:category|카테고리)\\s*:\\s*([^,\\s]+)");
    private static final Pattern PRICE = Pattern.compile("(?:price|가격)\\s*:\\s*([0-9,]+)(?:\\s*~\\s*([0-9,]+))?");

    public static HardFilter parse(String query) {
        if (query == null) return HardFilter.builder().build();
        String category = null;
        var m1 = CAT.matcher(query);
        if (m1.find()) {
            category = m1.group(1).trim();
        }

        BigDecimal min = null, max = null;
        var m2 = PRICE.matcher(query);
        if (m2.find()) {
            min = toMoney(m2.group(1));
            if (m2.group(2) != null) max = toMoney(m2.group(2));
        }
        return HardFilter.builder().category(category).minPrice(min).maxPrice(max).build();
    }

    private static BigDecimal toMoney(String s) {
        if (s == null) return null;
        return new BigDecimal(s.replace(",", ""));
    }
}