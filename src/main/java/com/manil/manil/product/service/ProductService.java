package com.manil.manil.product.service;

import com.manil.manil.gemini.client.EmbeddingClient;
import com.manil.manil.image.service.ImageStorageService;
import com.manil.manil.product.dto.request.ProductCreateRequest;
import com.manil.manil.product.dto.response.ProductDetailResponse;
import com.manil.manil.product.entity.AbstractTag;
import com.manil.manil.product.entity.AnalyzeCache;
import com.manil.manil.product.entity.Embedding;
import com.manil.manil.product.entity.Product;
import com.manil.manil.product.entity.ProductImage;
import com.manil.manil.product.repository.AbstractTagRepository;
import com.manil.manil.product.repository.AnalyzeCacheRepository;
import com.manil.manil.product.repository.EmbeddingRepository;
import com.manil.manil.product.repository.ProductRepository;
import com.pgvector.PGvector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ImageStorageService imageStorageService;
    private final EmbeddingRepository embeddingRepository;
    private final AbstractTagRepository abstractTagRepository;
    private final AnalyzeCacheRepository analyzeCacheRepository;
    private final EmbeddingClient embeddingClient;

    @Transactional
    public Long create(ProductCreateRequest req, List<MultipartFile> images) {
        // 1) 텍스트 저장
        Product p = Product.builder()
                .name(req.getName())
                .simpleDescription(req.getSimpleDescription())
                .detailedDescription(req.getDetailedDescription())
                .category(req.getCategory())
                .price(req.getPrice())
                .build();
        productRepository.save(p);

        // 2) 이미지 저장 (캐시 main + 업로드 images, mainIndex 반영)
        try {
            var urls = imageStorageService.finalizeProductImagesFromMultipart(
                    p.getId(),
                    req.getAnalyzeId(),
                    images,
                    req.getMainIndex()
            );

            // 파일명 재배치가 끝났으니 0번이 메인
            for (int i = 0; i < urls.size(); i++) {
                ProductImage pi = ProductImage.builder()
                        .product(p)
                        .url(urls.get(i))
                        .main(i == 0)
                        .sortOrder(i)
                        .build();
                p.getImages().add(pi); // Product.images 가 cascade = ALL 이면 save 불필요
            }
        } catch (Exception e) {
            // 이미지 저장 실패는 본문 저장과 분리 (로그만 남기고 계속)
        }

        upsertEmbeddingAndTags(p, req.getAnalyzeId());

        return p.getId();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getDetail(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        // DTO에 필요한 이미지 매핑 (sort_order ASC, 같은 값이면 id ASC)
        var images = p.getImages().stream()
                .sorted(Comparator
                        .comparingInt(ProductImage::getSortOrder)
                        .thenComparing(ProductImage::getId))
                .map(i -> ProductDetailResponse.ImageDto.builder()
                        .url(i.getUrl())
                        .isMain(i.isMain())
                        .sortOrder(i.getSortOrder())
                        .build())
                .toList();

        return ProductDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .simpleDescription(p.getSimpleDescription())
                .detailedDescription(p.getDetailedDescription())
                .category(p.getCategory())
                .price(p.getPrice())
                .keywords(null)     // 필요 시 별도 매핑
                .images(images)
                .build();
    }

    private void upsertEmbeddingAndTags(Product p, String analyzeId) {
        AnalyzeCache cache = null;
        if (analyzeId != null && !analyzeId.isBlank()) {
            try {
                cache = analyzeCacheRepository.findById(UUID.fromString(analyzeId)).orElse(null);
            } catch (IllegalArgumentException ignore) {
                // 잘못된 UUID 형식이면 캐시 미사용
            }
        }

        // ---- 임베딩 업서트 ----
        String vecText = null;

        // 1) 캐시가 있으면 캐시 임베딩 문자열 우선 사용
        if (cache != null && cache.getEmbeddingText() != null && !cache.getEmbeddingText().isBlank()) {
            vecText = cache.getEmbeddingText();
        }

        // 2) 없으면 상세설명으로 즉시 생성
        if (vecText == null && p.getDetailedDescription() != null && !p.getDetailedDescription().isBlank()) {
            float[] v = embeddingClient.embed(p.getDetailedDescription().strip());
            if (v != null && v.length == 768) {
                vecText = new PGvector(v).toString();
            }
        }

        if (vecText != null) {
            // 이미 있으면 수정, 없으면 생성
            Optional<Embedding> existing = embeddingRepository.findByProduct_Id(p.getId());
            Embedding emb = existing.orElseGet(() -> Embedding.builder().product(p).build());
            emb.setDescriptionEmbeddingText(vecText); // ColumnTransformer가 write 시 ::vector 적용
            embeddingRepository.save(emb);
        }

        // ---- 추상 태그 업서트 ----
        if (cache != null && cache.getAbstractTags() != null && cache.getAbstractTags().length > 0) {
            // 단순화: 모두 지우고 다시 삽입
            abstractTagRepository.deleteByProductId(p.getId());
            for (String tag : cache.getAbstractTags()) {
                if (tag == null || tag.isBlank()) continue;
                AbstractTag t = AbstractTag.builder()
                        .product(p)
                        .tag(tag.trim())
                        .build();
                abstractTagRepository.save(t);
            }
        }
    }
}