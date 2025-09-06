// src/main/java/com/manil/manil/product/service/AnalyzeService.java
package com.manil.manil.product.service;

import com.manil.manil.gemini.client.EmbeddingClient;
import com.manil.manil.gemini.client.LlmAnalyzeClient;
import com.manil.manil.gemini.config.GeminiProperties;
import com.manil.manil.product.dto.request.AnalyzeRequest;
import com.manil.manil.product.dto.response.AnalyzeResponse;
import com.manil.manil.product.entity.AnalyzeCache;
import com.manil.manil.product.repository.AnalyzeCacheRepository;
import com.manil.manil.product.util.InputHash;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyzeService {

    private final LlmAnalyzeClient llmAnalyzeClient;
    private final EmbeddingClient embeddingClient;
    private final AnalyzeCacheRepository cacheRepository;
    private final GeminiProperties gemProps;

    @Value("${manil.analyze.cache-ttl-hours:24}")
    private long cacheTtlHours;

    @Transactional
    public AnalyzeResponse analyze(AnalyzeRequest req) {
        final String inputHash = InputHash.compute(
                emptyToNull(req.getName()),
                emptyToNull(req.getSimpleDescription()),
                req.getKeywords(),
                emptyToNull(req.getCategory()),
                req.getPrice(),
                gemProps.getGenerateModel()
        );

        var cached = cacheRepository.findValidByInputHash(inputHash);
        if (cached.isPresent()) {
            var c = cached.get();
            return AnalyzeResponse.builder()
                    .detailedDescription(c.getDetailedDescription())
                    .analyzeId(c.getId().toString())
                    .build();
        }

        var input = new LlmAnalyzeClient.AnalyzeInput(
                emptyToNull(req.getName()),
                emptyToNull(req.getSimpleDescription()),
                req.getKeywords(),
                emptyToNull(req.getCategory()),
                req.getPrice()
        );
        var out = llmAnalyzeClient.extractAll(input);

        float[] descVec = embeddingClient.embed(out.detailedDescription());
        // (선택) 차원 검증
        if (descVec == null || descVec.length != 768) {
            throw new IllegalArgumentException("Embedding dimension must be 768, got " +
                    (descVec == null ? "null" : descVec.length));
        }

        AnalyzeCache entity = AnalyzeCache.builder()
                .id(UUID.randomUUID())
                .inputHash(inputHash)
                .requestName(input.name())
                .requestSimpleDescription(input.simpleDescription())
                .requestCategory(input.category())
                .requestPrice(input.price())
                .requestKeywords(listToArray(req.getKeywords()))
                .detailedDescription(out.detailedDescription())
                .ingredients(listToArray(out.ingredients()))
                .regions(listToArray(out.regions()))
                .ageGroups(listToArray(out.ageGroups()))
                .abstractTags(listToArray(out.abstractTags()))
                .modelName(gemProps.getGenerateModel())
                .expiresAt(OffsetDateTime.now().plusHours(cacheTtlHours))
                .build();

        // ★ PGvector로 세팅 (bytea 바인딩 방지)
        entity.setEmbeddingFromArray(descVec);

        try {
            cacheRepository.save(entity);
        } catch (DataIntegrityViolationException dup) {
            entity = cacheRepository.findByInputHash(inputHash).orElseThrow();
        }

        return AnalyzeResponse.builder()
                .detailedDescription(entity.getDetailedDescription())
                .analyzeId(entity.getId().toString())
                .build();
    }

    // ===== helpers =====
    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
    private static String[] listToArray(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list.toArray(String[]::new);
    }
}