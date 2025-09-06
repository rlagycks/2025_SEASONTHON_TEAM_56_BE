// src/main/java/com/manil/manil/search/service/SearchService.java
package com.manil.manil.search.service;

import com.manil.manil.gemini.client.EmbeddingClient;
import com.manil.manil.product.entity.Keyword;
import com.manil.manil.product.entity.ProductImage;
import com.manil.manil.product.repository.KeywordRepository;
import com.manil.manil.product.repository.ProductImageRepository;
import com.manil.manil.search.dto.SearchResponse;
import com.manil.manil.search.repository.SearchRepository;
import com.manil.manil.search.repository.SearchRow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final EmbeddingClient embeddingClient;
    private final SearchRepository searchRepository;
    private final KeywordRepository keywordRepository;
    private final ProductImageRepository productImageRepository;

    // yml에서 분리된 설정 사용
    @Value("${manil.search.repo-top-k:80}")
    private int repoTopK;

    @Value("${manil.search.response-top-k:3}")
    private int responseTopK;

    @Value("${manil.search.keyword-boost:0.02}")
    private double keywordBoost;

    public SearchResponse search(String query, String keywordsCsv) {
        // 1) 쿼리 임베딩
        final float[] qvec = embeddingClient.embed(query);
        final String qVecText = new com.pgvector.PGvector(qvec).toString();

        // 2) DB에서 넓게 후보 수집 (하드필터 null로 비활성화)
        final var rows = searchRepository.searchTopK(
                qVecText,
                null,   // category
                null,   // minPrice
                null,   // maxPrice
                repoTopK,
                0
        );

        // 3) 키워드 부스트 준비
        final Set<String> reqKeywords = parseKeywordsCsv(keywordsCsv);
        final List<Long> ids = rows.stream().map(SearchRow::getId).toList();

        final Map<Long, List<String>> productKeywords =
                (!ids.isEmpty() && !reqKeywords.isEmpty())
                        ? keywordRepository.findByProductIdIn(ids).stream()
                        .collect(Collectors.groupingBy(
                                k -> k.getProduct().getId(),
                                Collectors.mapping(Keyword::getKeyword, Collectors.toList())
                        ))
                        : Map.of();

        // 4) 매핑 + 소프트 스코어(키워드 부스트) + 메인 이미지
        final List<SearchResponse.ProductHit> hits = rows.stream()
                .map(r -> {
                    double sim = r.getSimilarity() == null ? 0.0 : r.getSimilarity();

                    if (!reqKeywords.isEmpty()) {
                        long matches = productKeywords.getOrDefault(r.getId(), List.of()).stream()
                                .filter(Objects::nonNull)
                                .map(String::toLowerCase)
                                .filter(reqKeywords::contains)
                                .count();
                        sim += matches * keywordBoost; // 가산점 반영
                    }

                    String mainUrl = productImageRepository
                            .findTopByProduct_IdOrderByMainDescSortOrderAscIdAsc(r.getId())
                            .map(ProductImage::getUrl)
                            .orElse(null);

                    return SearchResponse.ProductHit.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .description(summarize(r.getDetailedDescription()))
                            .price(r.getPrice())
                            .category(r.getCategory())
                            .similarity(sim)
                            .mainImageUrl(mainUrl)
                            .build();
                })
                .sorted(Comparator.comparing(
                        SearchResponse.ProductHit::similarity,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(responseTopK)  // 최종 응답 수 제한
                .toList();

        return SearchResponse.builder().products(hits).build();
    }

    private static Set<String> parseKeywordsCsv(String keywordsCsv) {
        if (keywordsCsv == null || keywordsCsv.isBlank()) return Set.of();
        return Arrays.stream(keywordsCsv.split(","))
                .map(String::trim).filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String summarize(String detailed) {
        if (detailed == null) return null;
        String s = detailed.strip();
        return s.length() <= 160 ? s : s.substring(0, 157) + "...";
    }
}