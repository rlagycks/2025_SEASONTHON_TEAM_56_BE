package com.manil.manil.entity;

import com.manil.manil.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor
public class Product extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "simple_description", columnDefinition = "text")
    private String simpleDescription;

    @Column(name = "detailed_description", columnDefinition = "text")
    private String detailedDescription;

    @Column(name = "category")
    private String category;

    @Column(name = "price", precision = 18, scale = 2)
    private BigDecimal price;

    public void init(String name, String simpleDesc, String detailedDesc, String category, BigDecimal price) {
        this.name = name;
        this.simpleDescription = simpleDesc;
        this.detailedDescription = detailedDesc;
        this.category = category;
        this.price = price;
    }
}
