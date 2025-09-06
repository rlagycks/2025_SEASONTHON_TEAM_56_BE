// com.manil.manil.product.repository.AbstractTagRepository.java
package com.manil.manil.product.repository;

import com.manil.manil.product.entity.AbstractTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;

public interface AbstractTagRepository extends JpaRepository<AbstractTag, Long> {
    List<AbstractTag> findByProductId(Long productId);
    List<AbstractTag> findByProductIdIn(Collection<Long> productIds);
    @Modifying
    void deleteByProductId(Long productId);
}