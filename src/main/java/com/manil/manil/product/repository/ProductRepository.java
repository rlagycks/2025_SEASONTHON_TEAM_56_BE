// com.manil.manil.product.repository.ProductRepository.java
package com.manil.manil.product.repository;

import com.manil.manil.product.entity.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 상세 조회 시 keywords 등을 즉시 필요로 하면 EntityGraph로 당겨와도 됨.
    @EntityGraph(attributePaths = { "embedding", "filters" })
    Optional<Product> findWithEmbeddingAndFiltersById(Long id);

}