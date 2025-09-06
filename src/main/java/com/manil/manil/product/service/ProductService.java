// src/main/java/com/manil/manil/product/service/ProductService.java
package com.manil.manil.product.service;

import com.manil.manil.product.dto.request.ProductCreateRequest;
import com.manil.manil.product.dto.response.ProductDetailResponse;
import com.manil.manil.product.entity.Product;
import com.manil.manil.product.entity.ProductImage;
import com.manil.manil.product.repository.ProductImageRepository;
import com.manil.manil.product.repository.ProductRepository;
import com.manil.manil.image.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ImageStorageService imageStorageService;

    @Transactional
    public Long create(ProductCreateRequest req) {
        Product p = Product.builder()
                .name(req.getName())
                .simpleDescription(req.getSimpleDescription())
                .detailedDescription(req.getDetailedDescription())
                .category(req.getCategory())
                .price(req.getPrice())
                .build();

        productRepository.save(p);

        int mainIndex = (req.getMainIndex() == null) ? 0 : Math.max(0, req.getMainIndex());

        try {
            List<String> urls = imageStorageService.finalizeProductImages(
                    p.getId(),
                    req.getAnalyzeId(),
                    req.getImageUrls(),
                    0
            );
            for (int i = 0; i < urls.size(); i++) {
                ProductImage pi = ProductImage.builder()
                        .product(p)
                        .url(urls.get(i))
                        .main(i == mainIndex)
                        .sortOrder(i)
                        .build();
                p.getImages().add(pi);
            }
        } catch (Exception e) {
            // 이미지 저장 실패 시 정책 결정(무시/롤백). 기본은 무시하고 텍스트만 등록.
        }

        return p.getId();
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse getDetail(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        return ProductDetailResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .simpleDescription(p.getSimpleDescription())
                .detailedDescription(p.getDetailedDescription())
                .category(p.getCategory())
                .price(p.getPrice())
                .keywords(null) // 필요 시 별도 테이블/컬럼에서 조회
                .build();
    }
}