// com.manil.manil.product.repository.ProductFiltersRepository.java
package com.manil.manil.product.repository;

import com.manil.manil.product.entity.ProductFilters;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface ProductFiltersRepository extends JpaRepository<ProductFilters, Long> {
    Optional<ProductFilters> findByProductId(Long productId);
    @Modifying
    void deleteByProductId(Long productId);
}