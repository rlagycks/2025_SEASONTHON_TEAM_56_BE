// src/main/java/com/manil/manil/product/service/ProductService.java
package com.manil.manil.product.service;

import com.manil.manil.image.service.ImageStorageService;
import com.manil.manil.product.dto.request.ProductCreateRequest;
import com.manil.manil.product.dto.response.ProductDetailResponse;
import com.manil.manil.product.entity.Product;
import com.manil.manil.product.entity.ProductImage;
import com.manil.manil.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;  // ← 추가
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ImageStorageService imageStorageService;

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
        }

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
                .images(images)     // ← 추가
                .build();
    }
}