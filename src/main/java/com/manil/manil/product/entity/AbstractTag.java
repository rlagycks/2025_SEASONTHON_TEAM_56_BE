package com.manil.manil.product.entity;

import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Id;

// com.manil.manil.product.domain.AbstractTag.java
@Entity
@Table(name="abstract_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbstractTag extends com.manil.manil.global.domain.BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="product_id", nullable=false)
    private Product product;

    @Column(nullable=false)
    private String tag;
}
