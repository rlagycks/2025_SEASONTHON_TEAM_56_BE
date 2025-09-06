// src/main/java/com/manil/manil/product/repository/ProductRepository.java
package com.manil.manil.product.repository;

import com.manil.manil.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = { "embedding", "filters" })
    Optional<Product> findWithEmbeddingAndFiltersById(Long id);

    @EntityGraph(attributePaths = { "images" })
    Optional<Product> findById(Long id);
}