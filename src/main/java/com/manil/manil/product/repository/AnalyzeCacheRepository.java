// src/main/java/com/manil/manil/product/repository/AnalyzeCacheRepository.java
package com.manil.manil.product.repository;

import com.manil.manil.product.entity.AnalyzeCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AnalyzeCacheRepository extends JpaRepository<AnalyzeCache, UUID> {

    Optional<AnalyzeCache> findByInputHash(String inputHash);

    @Query("""
           select c
             from AnalyzeCache c
            where c.inputHash = :hash
              and (c.expiresAt is null or c.expiresAt > CURRENT_TIMESTAMP)
           """)
    Optional<AnalyzeCache> findValidByInputHash(@Param("hash") String inputHash);

    long deleteByExpiresAtBefore(OffsetDateTime cutoff);
}