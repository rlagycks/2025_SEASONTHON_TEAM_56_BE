// com.manil.manil.product.support.Similarity.java
package com.manil.manil.product.util;

public final class Similarity {
    private Similarity() {}

    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null) throw new IllegalArgumentException("Vectors must not be null");
        if (a.length != b.length) throw new IllegalArgumentException("Vector dimensions mismatch");
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double)a[i] * b[i];
            na  += (double)a[i] * a[i];
            nb  += (double)b[i] * b[i];
        }
        if (na == 0.0 || nb == 0.0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}