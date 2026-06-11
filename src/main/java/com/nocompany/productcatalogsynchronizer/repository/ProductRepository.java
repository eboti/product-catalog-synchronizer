package com.nocompany.productcatalogsynchronizer.repository;

import com.nocompany.productcatalogsynchronizer.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Product entity providing database operations.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find a product by its ID.
     *
     * @param productId the product ID as string
     * @return Optional containing the Product if found
     */
    Optional<Product> findByProductId(String productId);

    /**
     * Find all products except those with the given IDs.
     * Used to identify products to delete during full-load synchronization.
     *
     * @param productIds list of product IDs to exclude
     * @return list of products not in the provided IDs
     */
    @Query("SELECT p FROM Product p WHERE p.productId NOT IN :productIds")
    List<Product> findProductsNotInIds(List<String> productIds);

    /**
     * Fetch all products for the given product IDs in a single query.
     * This allows callers to avoid N queries in a loop by prefetching existing rows.
     *
     * @param productIds list of product IDs to find
     * @return list of matching Product entities
     */
    List<Product> findByProductIdIn(List<String> productIds);

    /**
     * Check if a product with the given ID exists.
     *
     * @param productId the product ID
     * @return true if exists, false otherwise
     */
    boolean existsByProductId(String productId);
}

