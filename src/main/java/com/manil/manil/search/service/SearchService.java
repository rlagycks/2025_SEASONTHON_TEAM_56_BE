// src/main/java/com/manil/manil/search/service/SearchService.java
package com.manil.manil.search.service;

import com.manil.manil.gemini.client.EmbeddingClient;
import com.manil.manil.product.entity.Keyword;
import com.manil.manil.product.entity.ProductFilters;
import com.manil.manil.product.entity.ProductImage;
import com.manil.manil.product.repository.AbstractTagRepository;
import com.manil.manil.product.repository.KeywordRepository;
import com.manil.manil.product.repository.ProductFiltersRepository;
import com.manil.manil.product.repository.ProductImageRepository;
import com.manil.manil.search.dto.SearchResponse;
import com.manil.manil.search.filter.HardFilter;
import com.manil.manil.search.filter.HardFilterParser;
import com.manil.manil.search.repository.SearchRepository;
import com.manil.manil.search.repository.SearchRow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final EmbeddingClient embeddingClient;
    private final SearchRepository searchRepository;
    private final KeywordRepository keywordRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductFiltersRepository productFiltersRepository;
    private final AbstractTagRepository abstractTagRepository;

    // ====== 설정값 (application.yml) ======
    @Value("${manil.search.repo-top-k:200}")
    private int repoTopK;

    @Value("${manil.search.result-top-n:3}")
    private int resultTopN;

    @Value("${manil.search.weights.embedding:0.55}")
    private double wEmb;

    @Value("${manil.search.weights.keywords:0.15}")
    private double wKw;

    @Value("${manil.search.weights.abstract-tags:0.15}")
    private double wTag;

    @Value("${manil.search.weights.filters:0.10}")
    private double wFilt;

    @Value("${manil.search.weights.price:0.05}")
    private double wPrice;

    @Value("${manil.search.price.sigma:10000}")
    private double priceSigma;

    @Value("${manil.search.penalties.category-mismatch:0.05}")
    private double categoryMismatchPenalty;

    // 선택(키워드 점수에 쓰던 단순 부스트는 폐기하고 비율 점수로 대체)
    @Value("${manil.search.keyword-boost:0.02}")
    private double legacyKeywordBoost; // 사용하지 않음(호환용), 제거 가능

    public SearchResponse search(String query, String keywordsCsv) {
        // (A) 자연어에서 카테고리/가격 의도만 추출 → 하드필터 X, 소프트 스코어/패널티에서 사용
        final HardFilter hf = HardFilterParser.parse(query);

        // (B) 임베딩 생성
        final float[] qvec = embeddingClient.embed(query);
        final String qVecText = new com.pgvector.PGvector(qvec).toString();

        // 1) 임베딩 기반 넓게 후보 수집 (하드 WHERE 제거; 카테고리/가격은 null 전달)
        final List<SearchRow> rows = searchRepository.searchTopKNoFilter(
                qVecText, repoTopK, 0
        );

        if (rows.isEmpty()) {
            return SearchResponse.builder().products(List.of()).build();
        }

        // 2) 재랭킹용 보조 데이터 수집(배치)
        final List<Long> ids = rows.stream().map(SearchRow::getId).toList();

        // 2-1) 요청 키워드 파싱
        final Set<String> reqKeywords = parseKeywordsCsv(keywordsCsv);

        // 2-2) 상품 키워드
        final Map<Long, List<String>> productKeywords =
                (!ids.isEmpty() && !reqKeywords.isEmpty())
                        ? keywordRepository.findByProductIdIn(ids).stream()
                        .collect(Collectors.groupingBy(k -> k.getProduct().getId(),
                                Collectors.mapping(Keyword::getKeyword, Collectors.toList())))
                        : Map.of();

        // 2-3) 상품 필터(ingredients/regions/age_groups)
        final Map<Long, ProductFilters> productFilters =
                productFiltersRepository.findByProductIdIn(ids).stream()
                        .collect(Collectors.toMap(pf -> pf.getProduct().getId(), pf -> pf));

        final Map<Long, Set<String>> productTags =
                abstractTagRepository.findByProductIdIn(ids).stream()
                        .collect(Collectors.groupingBy(t -> t.getProduct().getId(),
                                Collectors.mapping(t -> t.getTag() == null ? null : t.getTag().toLowerCase(Locale.ROOT),
                                        Collectors.toSet())))
                        .entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                e -> {
                                    e.getValue().remove(null);
                                    return e.getValue();
                                }));

        // 3) 스코어 계산용 정규화 준비(임베딩 유사도 min-max 정규화)
        final double minEmb = rows.stream().map(SearchRow::getSimilarity).filter(Objects::nonNull)
                .min(Double::compare).orElse(0.0);
        final double maxEmb = rows.stream().map(SearchRow::getSimilarity).filter(Objects::nonNull)
                .max(Double::compare).orElse(1.0);
        final double embRange = Math.max(1e-9, maxEmb - minEmb);

        // 가격 의도가 있으면 중앙값(targetPrice) 산정
        final BigDecimal targetPrice = deriveTargetPrice(hf.minPrice(), hf.maxPrice());

        // 쿼리 토큰(태그/필터 매칭용): 전처리 간단 버전
        final Set<String> queryTokens = tokenize(query);

        // 4) 스코어링 + 메인 이미지 로딩
        List<SearchResponse.ProductHit> ranked = rows.stream().map(r -> {
                    // 4-1) 임베딩 점수(0~1)
                    final double embRaw = (r.getSimilarity() == null ? 0.0 : r.getSimilarity());
                    final double sEmb = (embRaw - minEmb) / embRange;

                    // 4-2) 키워드 점수(요청 키워드가 있을 때: 교집합 비율)
                    final double sKw = (!reqKeywords.isEmpty())
                            ? jaccardRatio(reqKeywords, toLowerSet(productKeywords.getOrDefault(r.getId(), List.of())))
                            : 0.0;

                    // 4-3) 필터 점수(쿼리 단어와 ingredients/regions/age_groups 교집합 비율)
                    final ProductFilters pf = productFilters.get(r.getId());
                    final Set<String> filterBag = new HashSet<>();
                    if (pf != null) {
                        addAllLower(filterBag, pf.getIngredients());
                        addAllLower(filterBag, pf.getRegions());
                        addAllLower(filterBag, pf.getAgeGroups());
                    }
                    final double sFilt = (!queryTokens.isEmpty() && !filterBag.isEmpty())
                            ? jaccardRatio(queryTokens, filterBag)
                            : 0.0;

                    final Set<String> tags = productTags.getOrDefault(r.getId(), Set.of());
                    final double sTag = (!queryTokens.isEmpty() && !tags.isEmpty())
                            ? jaccardRatio(queryTokens, tags) : 0.0;

                    // 4-4) 가격 점수(가우시안)
                    final double sPrice = (targetPrice != null && r.getPrice() != null)
                            ? gaussianPrice(r.getPrice(), targetPrice, priceSigma)
                            : 0.0;

                    // 4-5) 카테고리 패널티
                    double penalty = 0.0;
                    if (hf.category() != null && r.getCategory() != null
                            && !hf.category().equalsIgnoreCase(r.getCategory())) {
                        penalty += categoryMismatchPenalty; // [0,1] 범위에서 빼기
                    }

                    // 4-6) 최종 가중합
                    double finalScore =
                            wEmb * sEmb +
                                    wKw  * sKw  +
                                    wTag * sTag  +   // (추상 태그 테이블을 별도로 쓸 거면 여기 연결; 없으면 0 유지)
                                    wFilt* sFilt +
                                    wPrice*sPrice
                                    - penalty;

                    // 메인 이미지
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
                            .similarity(finalScore) // ← 노출용 점수: 최종 스코어
                            .mainImageUrl(mainUrl)
                            .build();
                })
                // 5) 재정렬
                .sorted(Comparator.comparing(
                        SearchResponse.ProductHit::similarity,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                // 6) 상위 N개만 반환
                .limit(resultTopN)
                .toList();

        return SearchResponse.builder().products(ranked).build();
    }

    // ====== helpers ======

    private static Set<String> parseKeywordsCsv(String keywordsCsv) {
        if (keywordsCsv == null || keywordsCsv.isBlank()) return Set.of();
        return Arrays.stream(keywordsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String summarize(String detailed) {
        if (detailed == null) return null;
        String s = detailed.strip();
        return s.length() <= 160 ? s : s.substring(0, 157) + "...";
    }

    private static BigDecimal deriveTargetPrice(BigDecimal min, BigDecimal max) {
        if (min != null && max != null) {
            return min.add(max).divide(BigDecimal.valueOf(2));
        } else if (min != null) {
            return min;
        } else if (max != null) {
            return max;
        }
        return null;
    }

    private static double gaussianPrice(BigDecimal price, BigDecimal target, double sigma) {
        double diff = price.subtract(target).doubleValue();
        double z = diff / Math.max(1e-9, sigma);
        return Math.exp(-(z * z)); // 0~1
    }

    private static Set<String> tokenize(String text) {
        if (text == null) return Set.of();
        String[] toks = text.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}]+");
        Set<String> set = new LinkedHashSet<>();
        for (String t : toks) if (!t.isBlank()) set.add(t);
        return set;
    }

    private static Set<String> toLowerSet(List<String> list) {
        if (list == null || list.isEmpty()) return Set.of();
        return list.stream()
                .filter(Objects::nonNull)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static void addAllLower(Set<String> bag, String[] arr) {
        if (arr == null) return;
        for (String s : arr) {
            if (s != null && !s.isBlank()) bag.add(s.toLowerCase(Locale.ROOT));
        }
    }

    private static double jaccardRatio(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }
}