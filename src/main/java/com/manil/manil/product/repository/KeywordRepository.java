// com.manil.manil.product.repository.KeywordRepository.java
package com.manil.manil.product.repository;

import com.manil.manil.product.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    List<Keyword> findByProductId(Long productId);
    List<Keyword> findByProductIdIn(Collection<Long> productIds);
     @Modifying
    void deleteByProductId(Long productId);
}