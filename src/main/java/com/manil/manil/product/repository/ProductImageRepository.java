// src/main/java/com/manil/manil/product/repository/ProductImageRepository.java
package com.manil.manil.product.repository;

import com.manil.manil.product.entity.Product;
import com.manil.manil.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductOrderBySortOrderAscIdAsc(Product product);
    Optional<ProductImage> findFirstByProductAndMainTrueOrderBySortOrderAscIdAsc(Product product);
}