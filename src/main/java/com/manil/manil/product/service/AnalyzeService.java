package com.manil.manil.product.service;

import com.manil.manil.gemini.client.EmbeddingClient;
import com.manil.manil.gemini.client.LlmAnalyzeClient;
import com.manil.manil.gemini.config.GeminiProperties;
import com.manil.manil.image.service.ImageStorageService;
import com.manil.manil.product.dto.request.AnalyzeRequest;
import com.manil.manil.product.dto.response.AnalyzeResponse;
import com.manil.manil.product.entity.AnalyzeCache;
import com.manil.manil.product.repository.AnalyzeCacheRepository;
import com.manil.manil.product.util.InputHash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeService {

    private final LlmAnalyzeClient llmAnalyzeClient;
    private final EmbeddingClient embeddingClient;
    private final AnalyzeCacheRepository cacheRepository;
    private final GeminiProperties gemProps;

    private final ImageStorageService imageStorageService;

    @Value("${manil.analyze.cache-ttl-hours:24}")
    private long cacheTtlHours;

    @Transactional
    public AnalyzeResponse analyze(AnalyzeRequest req,
                                   List<MultipartFile> multipartImages,
                                   boolean isMultipart) {

        // 1) 입력 해시로 캐시 조회 (이미지 포함 X: 텍스트 기반 캐시 정책 유지)
        final String inputHash = InputHash.compute(
                emptyToNull(req.getName()),
                emptyToNull(req.getSimpleDescription()),
                req.getKeywords(),
                emptyToNull(req.getCategory()),
                req.getPrice(),
                gemProps.getGenerateModel()
        );

        Optional<AnalyzeCache> cachedOpt = cacheRepository.findValidByInputHash(inputHash);
        AnalyzeCache entity;

        if (cachedOpt.isPresent()) {
            // 2-a) 캐시 HIT → 그대로 사용
            entity = cachedOpt.get();
        } else {
            // 2-b) 캐시 MISS → LLM 호출로 상세설명/필터/태그 산출
            var input = new LlmAnalyzeClient.AnalyzeInput(
                    emptyToNull(req.getName()),
                    emptyToNull(req.getSimpleDescription()),
                    req.getKeywords(),
                    emptyToNull(req.getCategory()),
                    req.getPrice()
            );
            var out = llmAnalyzeClient.extractAll(input);

            // 상세설명 임베딩 생성
            float[] descVec = embeddingClient.embed(out.detailedDescription());
            if (descVec == null || descVec.length != 768) {
                throw new IllegalArgumentException(
                        "Embedding dimension must be 768, got " + (descVec == null ? "null" : descVec.length)
                );
            }

            // 캐시 엔티티 생성
            entity = AnalyzeCache.builder()
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

            // ★ PGvector 문자열 세팅 (DB write 시 ::vector)
            entity.setEmbeddingFromArray(descVec);

            try {
                cacheRepository.save(entity);
            } catch (DataIntegrityViolationException dup) {
                // 동시성 등으로 이미 저장되었다면 재조회
                entity = cacheRepository.findByInputHash(inputHash).orElseThrow();
            }
        }

        // 3) 여기서 분석 ID 확정 (캐시 HIT/MISS 모두 동일)
        final String analyzeId = entity.getId().toString();

        // 4) 이미지 0번만 캐시에 저장 (실패해도 분석은 진행)
        try {
            if (isMultipart) {
                imageStorageService.saveAnalyzeMainFromMultipart(multipartImages, analyzeId);
            } else {
                imageStorageService.saveAnalyzeMainFromUrl(req.getImageUrls(), analyzeId);
            }
        } catch (Exception e) {
            log.warn("Analyze main image caching failed (analyzeId={}): {}", analyzeId, e.getMessage());
        }

        // 5) 응답 (상세설명 + analyze_id)
        return AnalyzeResponse.builder()
                .detailedDescription(entity.getDetailedDescription())
                .analyzeId(analyzeId)
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