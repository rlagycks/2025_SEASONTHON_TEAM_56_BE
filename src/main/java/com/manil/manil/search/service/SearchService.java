// com.manil.manil.search.service.SearchService.java
package com.manil.manil.search.service;

import com.manil.manil.gemini.client.EmbeddingClient;
import com.manil.manil.product.entity.Keyword;
import com.manil.manil.product.entity.ProductImage;
import com.manil.manil.product.repository.KeywordRepository;
import com.manil.manil.product.repository.ProductImageRepository;
import com.manil.manil.search.dto.SearchResponse;
import com.manil.manil.search.filter.HardFilter;
import com.manil.manil.search.filter.HardFilterParser;
import com.manil.manil.search.repository.SearchRepository;
import com.manil.manil.search.repository.SearchRow;
import com.pgvector.PGvector;
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

    @Value("${manil.search.top-k:3}")
    private int topK;

    @Value("${manil.search.keyword-boost:0.02}")
    private double keywordBoost;

    public SearchResponse search(String query, String keywordsCsv) {
        // 1) 하드 필터 파싱
        final HardFilter hf = HardFilterParser.parse(query);

        // 2) 쿼리 임베딩
        final float[] qvec = embeddingClient.embed(query);
        final String qVecText = new com.pgvector.PGvector(qvec).toString();

        // 3) 벡터 검색 (TOP-K)
        final List<SearchRow> rows = searchRepository.searchTopK(
                qVecText,                  // ← 문자열 파라미터
                hf.category(),
                hf.minPrice(),
                hf.maxPrice(),
                topK,
                0
        );

        // 4) 키워드 부스트 준비
        final Set<String> reqKeywords = parseKeywordsCsv(keywordsCsv); // 소문자+trim+중복제거
        final List<Long> ids = rows.stream().map(SearchRow::getId).toList();

        // 🔧 재할당 없이 한 번에 초기화해서 final로 유지
        final Map<Long, List<String>> productKeywords =
                (!ids.isEmpty() && !reqKeywords.isEmpty())
                        ? keywordRepository.findByProductIdIn(ids).stream()
                        .collect(Collectors.groupingBy(
                                k -> k.getProduct().getId(),
                                Collectors.mapping(Keyword::getKeyword, Collectors.toList())
                        ))
                        : Map.of();

        final List<SearchResponse.ProductHit> hits = rows.stream()
                .map(r -> {
                    double sim = r.getSimilarity() == null ? 0.0 : r.getSimilarity();

                    String mainUrl = productImageRepository
                            .findTopByProductIdOrderByMainDescSortOrderAscIdAsc(r.getId())
                            .map(ProductImage::getUrl)
                            .orElse(null);

                    return SearchResponse.ProductHit.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .description(summarize(r.getDetailedDescription()))
                            .price(r.getPrice())
                            .category(r.getCategory())
                            .similarity(sim)
                            .mainImageUrl(mainUrl) // ← 추가
                            .build();
                })
                .sorted(Comparator.comparing(
                        SearchResponse.ProductHit::similarity,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
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