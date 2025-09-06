// src/main/java/com/manil/manil/product/entity/Product.java
package com.manil.manil.product.entity;

import com.manil.manil.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name="products", schema = "app")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product extends BaseEntity {

    private String name;

    @Column(name="simple_description")
    private String simpleDescription;

    @Column(name="detailed_description")
    private String detailedDescription;

    private String category;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ProductFilters filters;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Embedding embedding;

    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();
}