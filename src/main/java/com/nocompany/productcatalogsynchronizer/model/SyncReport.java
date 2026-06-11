package com.nocompany.productcatalogsynchronizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Report of a product synchronization operation.
 * Contains metrics and samples of invalid products.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncReport implements Serializable {

    private Instant syncTime;

    /** Number of new products added to the database */
    private int productsAdded;

    /** Number of existing products updated in the database */
    private int productsUpdated;

    /** Number of products that had no changes during sync */
    private int productsUnchanged;

    /** Number of products deleted from the database */
    private int productsDeleted;

    /** Number of products skipped due to validation errors */
    private int productsSkipped;

    /** Sample of invalid products (up to 12) with their validation errors */
    private List<InvalidProductSample> invalidProductSamples;

    /**
     * Get total number of products processed (including skipped).
     * @return sum of added, updated, unchanged, and skipped products
     */
    public int getTotalProcessed() {
        return productsAdded + productsUpdated + productsUnchanged + productsSkipped;
    }

    /**
     * Get total number of products successfully synced (excluding skipped and deleted).
     * @return sum of added, updated, and unchanged products
     */
    public int getTotalSynced() {
        return productsAdded + productsUpdated + productsUnchanged;
    }
}

