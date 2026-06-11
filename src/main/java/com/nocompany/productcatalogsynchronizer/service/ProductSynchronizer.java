package com.nocompany.productcatalogsynchronizer.service;

import com.nocompany.productcatalogsynchronizer.model.SyncReport;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Interface for synchronizing product catalog from data feeds.
 * Implements full-load approach: only products from current feed remain in DB.
 */
public interface ProductSynchronizer {

    /**
     * Synchronize products from an input stream (file/feed).
     * This is a full-load approach - only products in the current feed will remain in DB.
     * When called directly (not from syncFromStreams), this resets statistics.
     *
     * @param inputStream the input stream containing CSV/TSV product data
     * @return true if synchronization was successful, false if there were processing errors
     * @throws IOException if there's an error reading the input
     */
    boolean syncFromStream(InputStream inputStream) throws IOException;

    /**
     * Synchronize products from multiple input streams in order.
     * Each stream is processed sequentially.
     *
     * @param inputStreams list of input streams in order of processing
     * @return synchronization report with statistics and samples
     * @throws IOException if there's an error reading any input
     */
    SyncReport syncFromStreams(List<InputStream> inputStreams) throws IOException;

    /**
     * Synchronize products from multiple file paths in order.
     *
     * @param filePaths list of file paths in order of processing
     * @return synchronization report with statistics and samples
     * @throws IOException if there's an error reading any file
     */
    SyncReport syncFromFiles(List<String> filePaths) throws IOException;

    /**
     * Get current synchronization statistics as a report (without finalizing).
     *
     * @return current synchronization report with statistics
     */
    SyncReport getCurrentReport();
}
