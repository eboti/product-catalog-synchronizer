package com.nocompany.productcatalogsynchronizer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Product entity representing a product in the catalog.
 * Corresponds to Google Product Data Feed specification fields.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id", unique = true)
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String availability;

    @Column(nullable = false)
    private String condition;

    @Column(nullable = false)
    private String price;

    @Column(name = "sale_price")
    private String salePrice;

    @Column(nullable = false)
    private String link;

    @Column(nullable = false)
    private String brand;

    @Column(name = "image_link")
    private String imageLink;

    @Column(name = "age_group")
    private String ageGroup;

    @Column(name = "google_product_category")
    private String googleProductCategory;

    /**
     * Timestamp when this product was first added to the database.
     * This is set once and never updated in subsequent synchronizations.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp of the last synchronization when this product was processed.
     * This is updated on each synchronization.
     */
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastUpdatedAt == null) {
            lastUpdatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}

