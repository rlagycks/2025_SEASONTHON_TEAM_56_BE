package com.manil.manil.product;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;       // 상품명
    private String category;   // 카테고리
    private String region;     // 지역/원산지
    @Column(length = 1000)
    private String tags;       // 콤마구분 태그
    @Column(length = 2000)
    private String features;   // 특징 요약
    private Integer price;     // 원화 가격(선택)

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
}
