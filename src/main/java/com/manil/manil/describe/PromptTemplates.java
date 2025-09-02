package com.manil.manil.describe;

import com.manil.manil.product.Product;

public final class PromptTemplates {

    public static String describeTemplate(Product p, String userHint, String locale) {
        String base = """
        You are a product expert copywriter.
        Write in language: %s.
        Use ONLY the facts provided. Do NOT invent specs.

        PRODUCT_FACTS:
        - name: %s
        - category: %s
        - region: %s
        - tags: %s
        - features: %s
        - priceKRW: %s

        USER_HINT: %s

        OUTPUT STRICTLY AS VALID JSON with fields:
        {
          "headline": "string",
          "pros": ["string", ...],
          "cons": ["string", ...],
          "regionalNotes": "string",
          "useCases": ["string", ...],
          "fullText": "string"
        }
        No markdown. No commentary. JSON only.
        """;
        return String.format(
                base,
                locale == null ? "ko" : locale,
                ns(p.getName()),
                ns(p.getCategory()),
                ns(p.getRegion()),
                ns(p.getTags()),
                ns(p.getFeatures()),
                p.getPrice() == null ? "" : p.getPrice().toString(),
                ns(userHint)
        );
    }

    private static String ns(String s) { return s == null ? "" : s; }

    private PromptTemplates() {}
}
