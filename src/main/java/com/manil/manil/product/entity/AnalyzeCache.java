// src/main/java/com/manil/manil/product/entity/AnalyzeCache.java
package com.manil.manil.product.entity;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analyze_cache", schema = "app",
        indexes = @Index(name = "uk_analyze_cache_input_hash", columnList = "input_hash", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnalyzeCache {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "input_hash", nullable = false, unique = true, length = 128)
    private String inputHash;

    @Column(name = "request_name")
    private String requestName;

    @Column(name = "request_simple_description", columnDefinition = "text")
    private String requestSimpleDescription;

    @Column(name = "request_category")
    private String requestCategory;

    @Column(name = "request_price", precision = 12, scale = 2)
    private BigDecimal requestPrice;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "request_keywords", columnDefinition = "text[]")
    private String[] requestKeywords;

    @Column(name = "detailed_description", nullable = false, columnDefinition = "text")
    private String detailedDescription;

    /** 벡터 컬럼을 String으로 들고, DB에서만 ::vector 캐스팅 */
    @Column(name = "embedding", columnDefinition = "vector(768)")
    @ColumnTransformer(write = "?::vector", read = "embedding::text")
    private String embeddingText;

    @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "ingredients",   columnDefinition = "text[]")
    private String[] ingredients;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "regions",       columnDefinition = "text[]")
    private String[] regions;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "age_groups",    columnDefinition = "text[]")
    private String[] ageGroups;
    @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "abstract_tags", columnDefinition = "text[]")
    private String[] abstractTags;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "expires_at", columnDefinition = "timestamptz")
    private OffsetDateTime expiresAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (expiresAt == null) expiresAt = OffsetDateTime.now().plusHours(24);
    }

    public void setEmbeddingFromArray(float[] arr) {
        this.embeddingText = (arr == null) ? null : new PGvector(arr).toString();
    }

    public float[] getEmbeddingAsArray() {
        if (embeddingText == null) return null;
        try {
            return new PGvector(embeddingText).toArray();
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Invalid vector text for analyze_cache.embedding", e);
        }
    }
}