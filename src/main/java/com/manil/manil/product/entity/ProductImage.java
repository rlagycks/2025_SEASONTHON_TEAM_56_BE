// src/main/java/com/manil/manil/product/entity/ProductImage.java
package com.manil.manil.product.entity;

import com.manil.manil.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images", schema = "app")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 1000)
    private String url;          // 공개 URL (/images/products/{id}/001.jpg)

    @Column(name = "is_main", nullable = false)
    private boolean main;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}