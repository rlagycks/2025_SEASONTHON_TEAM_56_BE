// src/main/java/com/manil/manil/product/repository/EmbeddingRepository.java
package com.manil.manil.product.repository;

import com.manil.manil.product.entity.Embedding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmbeddingRepository extends JpaRepository<Embedding, Long> {
    boolean existsByProduct_Id(Long productId);
}