// src/main/java/com/manil/manil/search/repository/SearchRepository.java
package com.manil.manil.search.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SearchRepository extends Repository<com.manil.manil.product.entity.Product, Long> {

    @Query(value = """
        SELECT
            p.id AS id,
            p.name AS name,
            p.detailed_description AS detailedDescription,
            p.price AS price,
            p.category AS category,
            1 - (e.description_embedding <-> CAST(:queryVec AS vector)) AS similarity
        FROM app.embeddings e
        JOIN app.products p ON p.id = e.product_id
        WHERE (:category IS NULL OR p.category = :category)
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
        ORDER BY similarity DESC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<SearchRow> searchTopK(
            @Param("queryVec") String queryVec,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("limit") int limit,
            @Param("offset") int offset
    );
}