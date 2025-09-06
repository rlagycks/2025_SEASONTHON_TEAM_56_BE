package com.manil.manil.product.entity;

import com.manil.manil.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product extends BaseEntity {

    private String name;

    @Column(name="simple_description")
    private String simpleDescription;

    @Column(name="detailed_description")
    private String detailedDescription;

    private String category;

    @Column(precision = 12, scale = 2)
    private java.math.BigDecimal price;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ProductFilters filters;

    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Embedding embedding;
}