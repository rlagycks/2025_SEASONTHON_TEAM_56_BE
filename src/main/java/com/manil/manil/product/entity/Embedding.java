// src/main/java/com/manil/manil/product/entity/Embedding.java
package com.manil.manil.product.entity;

import com.pgvector.PGvector;
import com.manil.manil.global.domain.BaseEntity; // ★ 상속
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "embeddings", schema = "app")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Embedding extends BaseEntity { // ★ BaseEntity 상속

    @OneToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    /** 벡터는 문자열로 보관, DB 쓰기 시에만 ::vector 캐스팅 */
    @Column(name = "description_embedding", columnDefinition = "vector(768)")
    @org.hibernate.annotations.ColumnTransformer(
            write = "?::vector",
            read = "description_embedding::text"
    )
    private String descriptionEmbeddingText;

    public void setDescriptionEmbeddingFromArray(float[] arr) {
        this.descriptionEmbeddingText = (arr == null) ? null : new PGvector(arr).toString();
    }

    public float[] getDescriptionEmbeddingAsArray() {
        if (descriptionEmbeddingText == null) return null;
        try {
            return new PGvector(descriptionEmbeddingText).toArray();
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Invalid vector text for description_embedding", e);
        }
    }
}