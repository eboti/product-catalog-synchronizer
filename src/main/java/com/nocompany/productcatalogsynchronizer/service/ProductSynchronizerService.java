package com.nocompany.productcatalogsynchronizer.service;

import com.nocompany.productcatalogsynchronizer.model.InvalidProductSample;
import com.nocompany.productcatalogsynchronizer.model.Product;
import com.nocompany.productcatalogsynchronizer.model.SyncReport;
import com.nocompany.productcatalogsynchronizer.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main service for synchronizing product catalog from data feeds.
 * Implements full-load approach: only products from current feed remain in DB.
 */
@Service
@RequiredArgsConstructor
public class ProductSynchronizerService implements ProductSynchronizer {

    private final ProductRepository productRepository;
    private final CsvParserService csvParserService;
    private final ProductValidator productValidator;
    private final ApplicationContext applicationContext;

    // Track statistics
    private int productsAdded = 0;
    private int productsUpdated = 0;
    private int productsUnchanged = 0;
    private int productsDeleted = 0;
    private int productsSkipped = 0;

    private final List<InvalidProductSample> invalidProductSamples = new ArrayList<>();
    private final Set<String> processedProductIds = new HashSet<>();

    /**
     * Synchronize products from an input stream (file/feed).
     * This is a full-load approach - only products in the current feed will remain in DB.
     * When called directly (not from syncFromStreams), this resets statistics.
     *
     * @param inputStream the input stream containing CSV/TSV product data
     * @return true if synchronization was successful, false if there were processing errors
     * @throws IOException if there's an error reading the input
     */
    @Transactional
    public boolean syncFromStream(InputStream inputStream) throws IOException {
        // For direct calls, reset statistics
        // For calls from syncFromStreams, the parent method handles resetting
        return syncFromStreamInternal(inputStream, true);
    }

    /**
     * Internal synchronization method that can control statistics reset.
     *
     * @param inputStream the input stream containing CSV/TSV product data
     * @param resetStats whether to reset statistics before sync
     * @return true if synchronization was successful, false if there were processing errors
     * @throws IOException if there's an error reading the input
     */
    @Transactional
    protected boolean syncFromStreamInternal(InputStream inputStream, boolean resetStats) throws IOException {
        if (resetStats) {
            resetStatistics();
        }
        // Parse the CSV/TSV file
        List<Map<String, String>> parsedProducts = csvParserService.parseProducts(inputStream);

        // Process each product
        List<Product> productsToSave = new ArrayList<>();

        // Optimize DB access: prefetch existing products for all product IDs present in the feed
        // so we avoid issuing a query per row inside the loop.
        Set<String> idsToFetch = parsedProducts.stream()
            .map(m -> m.get("id"))
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());

        Map<String, Product> existingById = new HashMap<>();
        if (!idsToFetch.isEmpty()) {
            List<Product> existingProducts = productRepository.findByProductIdIn(new ArrayList<>(idsToFetch));
            existingById = existingProducts.stream().collect(Collectors.toMap(Product::getProductId, p -> p));
        }

        for (Map<String, String> productData : parsedProducts) {
            // Validate the product
            List<String> errors = productValidator.validate(productData);

            if (!errors.isEmpty()) {
                // Invalid product - skip and record sample
                productsSkipped++;
                if (invalidProductSamples.size() < 12) {
                    String rawData = formatRawData(productData);
                    InvalidProductSample sample = InvalidProductSample.builder()
                        .rawData(rawData)
                        .errors(errors)
                        .build();
                    invalidProductSamples.add(sample);
                }
                continue;
            }

            // Valid product - map to entity
            String productId = productData.get("id").trim();
            processedProductIds.add(productId);

            Product product = mapToProduct(productData);

            // Check if this product already exists (from prefetched map)
            Product existing = existingById.get(productId);

            if (existing != null) {

                // Check if product data changed
                if (hasProductChanged(existing, product)) {
                    // Update existing product but keep original createdAt
                    existing.setTitle(product.getTitle());
                    existing.setDescription(product.getDescription());
                    existing.setAvailability(product.getAvailability());
                    existing.setCondition(product.getCondition());
                    existing.setPrice(product.getPrice());
                    existing.setSalePrice(product.getSalePrice());
                    existing.setLink(product.getLink());
                    existing.setBrand(product.getBrand());
                    existing.setImageLink(product.getImageLink());
                    existing.setAgeGroup(product.getAgeGroup());
                    existing.setGoogleProductCategory(product.getGoogleProductCategory());
                    existing.setLastUpdatedAt(Instant.now());

                    productsToSave.add(existing);
                    productsUpdated++;
                } else {
                    // No changes
                    productsUnchanged++;
                }
            } else {
                // New product
                productsToSave.add(product);
                productsAdded++;
            }
        }

        // Perform bulk save of new/updated products
        if (!productsToSave.isEmpty()) {
            productRepository.saveAll(productsToSave);
        }

        // Delete products that were not in this sync (full-load approach)
        if (!processedProductIds.isEmpty()) {
            List<Product> productsToDelete = productRepository.findProductsNotInIds(
                new ArrayList<>(processedProductIds)
            );

            if (!productsToDelete.isEmpty()) {
                productsDeleted = productsToDelete.size();
                productRepository.deleteAll(productsToDelete);
            }
        }

        return true;
    }

    /**
     * Synchronize products from multiple input streams in order.
     * Each stream is processed sequentially.
     *
     * @param inputStreams list of input streams in order of processing
     * @return synchronization report with statistics and samples
     * @throws IOException if there's an error reading any input
     */
    public SyncReport syncFromStreams(List<InputStream> inputStreams) throws IOException {
        // Process each input stream in its own transaction.
        // We call the public transactional entry point via the Spring proxy to ensure each file is committed separately.
        int totalAdded = 0;
        int totalUpdated = 0;
        int totalUnchanged = 0;
        int totalDeleted = 0;
        int totalSkipped = 0;
        List<InvalidProductSample> aggregatedInvalidSamples = new ArrayList<>();

        for (InputStream inputStream : inputStreams) {
            // Obtain proxy to this service to ensure @Transactional on syncFromStream is applied
            ProductSynchronizerService proxy = applicationContext.getBean(ProductSynchronizerService.class);
            proxy.syncFromStream(inputStream);

            // After processing, read the report for this run and aggregate
            SyncReport perRun = proxy.getCurrentReport();
            totalAdded += perRun.getProductsAdded();
            totalUpdated += perRun.getProductsUpdated();
            totalUnchanged += perRun.getProductsUnchanged();
            totalDeleted += perRun.getProductsDeleted();
            totalSkipped += perRun.getProductsSkipped();

            // collect up to 12 samples in total
            for (InvalidProductSample s : perRun.getInvalidProductSamples()) {
                if (aggregatedInvalidSamples.size() < 12) {
                    aggregatedInvalidSamples.add(s);
                }
            }
        }

        // Build aggregated report
        return SyncReport.builder()
            .syncTime(Instant.now())
            .productsAdded(totalAdded)
            .productsUpdated(totalUpdated)
            .productsUnchanged(totalUnchanged)
            .productsDeleted(totalDeleted)
            .productsSkipped(totalSkipped)
            .invalidProductSamples(aggregatedInvalidSamples)
            .build();
    }

    /**
     * Synchronize products from multiple file paths in order.
     *
     * @param filePaths list of file paths in order of processing
     * @return synchronization report with statistics and samples
     * @throws IOException if there's an error reading any file
     */
    public SyncReport syncFromFiles(List<String> filePaths) throws IOException {
        List<InputStream> inputStreams = new ArrayList<>();
        for (String filePath : filePaths) {
            inputStreams.add(new java.io.FileInputStream(filePath));
        }

        try {
            return syncFromStreams(inputStreams);
        } finally {
            // Close all input streams
            for (InputStream stream : inputStreams) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Log but continue closing other streams
                }
            }
        }
    }

    /**
     * Map a CSV row (map) to a Product entity.
     */
    private Product mapToProduct(Map<String, String> data) {
        Product product = new Product();
        product.setProductId(data.getOrDefault("id", "").trim());
        product.setTitle(data.getOrDefault("title", "").trim());
        product.setDescription(data.getOrDefault("description", "").trim());
        product.setAvailability(data.getOrDefault("availability", "").trim().toLowerCase());
        product.setCondition(data.getOrDefault("condition", "").trim().toLowerCase());
        product.setPrice(data.getOrDefault("price", "").trim());
        product.setSalePrice(data.getOrDefault("sale_price", "").trim());
        product.setLink(data.getOrDefault("link", "").trim());
        product.setBrand(data.getOrDefault("brand", "").trim());
        product.setImageLink(data.getOrDefault("image_link", "").trim());
        product.setAgeGroup(data.getOrDefault("age_group", "").trim());
        product.setGoogleProductCategory(data.getOrDefault("google_product_category", "").trim());

        if (product.getCreatedAt() == null) {
            product.setCreatedAt(Instant.now());
        }
        if (product.getLastUpdatedAt() == null) {
            product.setLastUpdatedAt(Instant.now());
        }

        return product;
    }

    /**
     * Check if product data has changed from the existing record.
     */
    private boolean hasProductChanged(Product existing, Product newProduct) {
        return !Objects.equals(existing.getTitle(), newProduct.getTitle()) ||
               !Objects.equals(existing.getDescription(), newProduct.getDescription()) ||
               !Objects.equals(existing.getAvailability(), newProduct.getAvailability()) ||
               !Objects.equals(existing.getCondition(), newProduct.getCondition()) ||
               !Objects.equals(existing.getPrice(), newProduct.getPrice()) ||
               !Objects.equals(existing.getSalePrice(), newProduct.getSalePrice()) ||
               !Objects.equals(existing.getLink(), newProduct.getLink()) ||
               !Objects.equals(existing.getBrand(), newProduct.getBrand()) ||
               !Objects.equals(existing.getImageLink(), newProduct.getImageLink()) ||
               !Objects.equals(existing.getAgeGroup(), newProduct.getAgeGroup()) ||
               !Objects.equals(existing.getGoogleProductCategory(), newProduct.getGoogleProductCategory());
    }

    /**
     * Format product data as a string for reporting.
     */
    private String formatRawData(Map<String, String> data) {
        return data.entrySet().stream()
            .map(e -> e.getKey() + "=" + (e.getValue() != null ? e.getValue() : ""))
            .collect(Collectors.joining(", "));
    }

    /**
     * Generate synchronization report.
     */
    private SyncReport generateReport() {
        return SyncReport.builder()
            .syncTime(Instant.now())
            .productsAdded(productsAdded)
            .productsUpdated(productsUpdated)
            .productsUnchanged(productsUnchanged)
            .productsDeleted(productsDeleted)
            .productsSkipped(productsSkipped)
            .invalidProductSamples(new ArrayList<>(invalidProductSamples))
            .build();
    }

    /**
     * Reset statistics for a new synchronization.
     */
    private void resetStatistics() {
        productsAdded = 0;
        productsUpdated = 0;
        productsUnchanged = 0;
        productsDeleted = 0;
        productsSkipped = 0;
        invalidProductSamples.clear();
        processedProductIds.clear();
    }

    /**
     * Get current synchronization statistics as a report (without finalizing).
     */
    public SyncReport getCurrentReport() {
        return SyncReport.builder()
            .syncTime(Instant.now())
            .productsAdded(productsAdded)
            .productsUpdated(productsUpdated)
            .productsUnchanged(productsUnchanged)
            .productsDeleted(productsDeleted)
            .productsSkipped(productsSkipped)
            .invalidProductSamples(new ArrayList<>(invalidProductSamples))
            .build();
    }
}

