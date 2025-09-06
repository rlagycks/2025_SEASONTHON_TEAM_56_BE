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
    p.id,
    p.name,
    p.detailed_description AS detailedDescription,
    p.price,
    p.category,
    1 - (e.description_embedding <=> CAST(:queryVec AS vector)) / 2.0 AS similarity
    FROM app.embeddings e
    JOIN app.products p ON p.id = e.product_id
    ORDER BY e.description_embedding <=> CAST(:queryVec AS vector) ASC
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<SearchRow> searchTopKNoFilter(@Param("queryVec") String queryVec,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);
}