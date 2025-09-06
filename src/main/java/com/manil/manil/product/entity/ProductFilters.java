package com.manil.manil.product.entity;

import com.manil.manil.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_filters")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProductFilters extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "ingredients", columnDefinition = "text[]")
    private String[] ingredients;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "regions", columnDefinition = "text[]")
    private String[] regions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "age_groups", columnDefinition = "text[]")
    private String[] ageGroups;
}