// src/main/java/com/manil/manil/product/service/ProductService.java
package com.manil.manil.product.service;

import com.manil.manil.gemini.client.EmbeddingClient;
import com.manil.manil.gemini.client.LlmAnalyzeClient;
import com.manil.manil.global.exception.BusinessException;
import com.manil.manil.global.exception.error.BaseError;
import com.manil.manil.product.dto.request.ProductCreateRequest;
import com.manil.manil.product.dto.response.ProductDetailResponse;
import com.manil.manil.product.entity.*;
import com.manil.manil.product.repository.*;
import com.manil.manil.product.util.Similarity;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductFiltersRepository productFiltersRepository;
    private final KeywordRepository keywordRepository;
    private final AbstractTagRepository abstractTagRepository;
    private final EmbeddingRepository embeddingRepository;

    private final AnalyzeCacheRepository analyzeCacheRepository;
    private final EmbeddingClient embeddingClient;
    private final LlmAnalyzeClient llmAnalyzeClient;

    /** 유사도 임계값 (예: 0.92). application.yml 에서 주입 */
    @Value("${manil.ai.sim-threshold:0.92}")
    private double simThreshold;

    @Transactional
    public Long create(ProductCreateRequest req) {
        // 0) 입력 정리
        var keywords = normalizedKeywords(req.getKeywords());

        // 1) 상세설명 기반 임베딩 1회 생성 (제품에 최종 저장할 기준)
        float[] newEmbedding = embeddingClient.embed(req.getDetailedDescription());

        // 2) analyze 캐시 조회(유효기간 내만 인정)
        AnalyzeCache cache = null;
        if (req.getAnalyzeId() != null && !req.getAnalyzeId().isBlank()) {
            try {
                UUID id = UUID.fromString(req.getAnalyzeId());
                cache = analyzeCacheRepository.findById(id)
                        .filter(c -> c.getExpiresAt() == null || c.getExpiresAt().isAfter(OffsetDateTime.now()))
                        .orElse(null);
            } catch (IllegalArgumentException ignored) {
                // analyzeId 포맷이 잘못된 경우는 캐시 없음으로 처리
            }
        }

        // 3) 재분석 여부 판정 (널/차원 불일치 방어)
        boolean shouldReanalyze;
        if (cache == null) {
            shouldReanalyze = true;
        } else {
            float[] cached = cache.getEmbeddingAsArray(); // ← 배열로 꺼내기
            if (cached == null || cached.length != newEmbedding.length) {
                shouldReanalyze = true;
            } else {
                double sim = Similarity.cosine(newEmbedding, cached);
                shouldReanalyze = sim < simThreshold;
            }
        }

        // 4) 필터/태그 결정 (임베딩은 newEmbedding을 그대로 저장)
        String[] ingredients;
        String[] regions;
        String[] ageGroups;
        String[] abstractTags;

        if (shouldReanalyze) {
            // 캐시 없음 or 유사도 낮음 → LLM 재호출로 정합 데이터 획득
            var out = llmAnalyzeClient.extractAll(new LlmAnalyzeClient.AnalyzeInput(
                    req.getName(),
                    req.getSimpleDescription(),
                    req.getKeywords(),
                    req.getCategory(),
                    req.getPrice()
            ));
            ingredients  = toArray(out.ingredients());
            regions      = toArray(out.regions());
            ageGroups    = toArray(out.ageGroups());
            abstractTags = toArray(out.abstractTags());
        } else {
            // 캐시 재사용
            ingredients  = cache.getIngredients();
            regions      = cache.getRegions();
            ageGroups    = cache.getAgeGroups();
            abstractTags = cache.getAbstractTags();
        }

        // 5) 저장 트랜잭션
        Product product = Product.builder()
                .name(req.getName())
                .simpleDescription(req.getSimpleDescription())
                .detailedDescription(req.getDetailedDescription())
                .category(req.getCategory())
                .price(req.getPrice())
                .build();
        productRepository.save(product);

        // filters
        ProductFilters filters = ProductFilters.builder()
                .product(product)
                .ingredients(ingredients)
                .regions(regions)
                .ageGroups(ageGroups)
                .build();
        productFiltersRepository.save(filters);

        // keywords
        if (keywords != null) {
            for (String k : keywords) {
                keywordRepository.save(Keyword.builder()
                        .product(product)
                        .keyword(k)
                        .build());
            }
        }

        // abstract tags
        if (abstractTags != null) {
            for (String t : abstractTags) {
                abstractTagRepository.save(AbstractTag.builder()
                        .product(product)
                        .tag(t)
                        .build());
            }
        }

        // 6) embedding
        Embedding emb = Embedding.builder()
                .product(product)
                .build();
// ★ float[] → 문자열로 변환해서 세팅
        emb.setDescriptionEmbeddingFromArray(newEmbedding);

        embeddingRepository.save(emb);

        return product.getId();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getDetail(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(new BaseError() {
                    @Override public HttpStatus getHttpStatus() { return HttpStatus.NOT_FOUND; }
                    @Override public String getMessage() { return "상품을 찾을 수 없습니다."; }
                }));

        var kws = keywordRepository.findByProductId(id).stream()
                .map(Keyword::getKeyword)
                .toList();

        return ProductDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .simpleDescription(p.getSimpleDescription())
                .keywords(kws)
                .detailedDescription(p.getDetailedDescription())
                .category(p.getCategory())
                .price(p.getPrice())
                .build();
    }

    // ===== helpers =====
    private static String[] toArray(List<String> list) {
        return (list == null || list.isEmpty()) ? null : list.toArray(String[]::new);
    }

    private static List<String> normalizedKeywords(List<String> keywords) {
        if (keywords == null) return null;
        return keywords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(50)
                .toList();
    }
}