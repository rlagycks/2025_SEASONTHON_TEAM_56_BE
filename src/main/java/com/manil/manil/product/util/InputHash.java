// com.manil.manil.product.util.InputHash
package com.manil.manil.product.util;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

public final class InputHash {
    private InputHash(){}

    public static String compute(String name, String simple, List<String> keywords,
                                 String category, BigDecimal price,
                                 String modelName) {
        String canonical = "name=" + norm(name) +
                "|simple=" + norm(simple) +
                "|keywords=" + normKeywords(keywords) +
                "|category=" + norm(category) +
                "|price=" + normPrice(price) +
                "|model=" + safe(modelName);   // ✅ promptVersion 제외
        return sha256(canonical);
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
    private static String normKeywords(List<String> ks) {
        if (ks == null || ks.isEmpty()) return "";
        return ks.stream().filter(x -> x != null && !x.isBlank())
                .map(InputHash::norm).sorted()
                .reduce((a,b) -> a + "," + b).orElse("");
    }
    private static String normPrice(BigDecimal p) {
        return p == null ? "" : p.stripTrailingZeros().toPlainString();
    }
    private static String safe(String s) { return s == null ? "" : s; }

    private static String sha256(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}