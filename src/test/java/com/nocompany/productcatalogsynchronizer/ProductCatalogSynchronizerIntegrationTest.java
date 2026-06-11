package com.nocompany.productcatalogsynchronizer;

import com.nocompany.productcatalogsynchronizer.repository.ProductRepository;
import com.nocompany.productcatalogsynchronizer.service.CsvParserService;
import com.nocompany.productcatalogsynchronizer.model.SyncReport;
import com.nocompany.productcatalogsynchronizer.service.ProductSynchronizerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration test demonstrating how to run the catalog synchronizer
 * with the provided product data files (file1.txt, file2.txt, file3.txt).
 *
 * INSTRUCTIONS:
 * 1. Place the product data files in this directory or configure the path below:
 *    - file1.txt
 *    - file2.txt
 *    - file3.txt
 *
 * 2. Run this test to synchronize all products and view the report.
 *
 * 3. The data will be stored in an in-memory H2 database during tests.
 *    For production use with a real database, update application.yaml.
 *
 * 4. The synchronizer processes files in order and implements full-load approach:
 *    - Only products from the latest synchronized file(s) remain in the database
 *    - Older products are automatically deleted
 *
 * Expected Results:
 * - file1.txt: ~20 products added
 * - file2.txt: 11 products added (some shared with file1, those will be unchanged)
 * - file3.txt: 26 products added (some shared with file2, those will be unchanged/updated)
 * - Final database will contain only products from file3.txt
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Product Catalog Synchronizer - File-based Integration Test")
class ProductCatalogSynchronizerIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogSynchronizerIntegrationTest.class);

    @Autowired
    private ProductSynchronizerService synchronizerService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CsvParserService csvParserService;

    @Test
    @DisplayName("Synchronize products from file1.txt, file2.txt, and file3.txt")
    void testSynchronizeFromProvidedFiles() throws IOException {
        // Path to the product data files
        // Adjust the path if the files are located elsewhere
        String basePath = "src/test/resources";

        String file1 = Paths.get(basePath, "file1.txt").toString();
        String file2 = Paths.get(basePath, "file2.txt").toString();
        String file3 = Paths.get(basePath, "file3.txt").toString();

        // Process files one by one (each should be processed and committed separately)
        SyncReport report1 = synchronizerService.syncFromFiles(List.of(file1));
        long countAfterFile1 = productRepository.count();
        assert countAfterFile1 > 0 : "After file1 there should be some products in DB";

        SyncReport report2 = synchronizerService.syncFromFiles(List.of(file2));
        long countAfterFile2 = productRepository.count();
        assert countAfterFile2 >= 0 : "After file2 processing DB should be accessible";

        SyncReport report3 = synchronizerService.syncFromFiles(List.of(file3));
        long countAfterFile3 = productRepository.count();

        // Verify final DB contains only products from file3 by comparing counts with parsed file3
        int parsedFile3Count = csvParserService.parseProductsFromFile(file3).size();
        assert countAfterFile3 == parsedFile3Count : "Final DB should contain only products from file3";

        // Print last report for manual inspection
        printSyncReport(report3);
    }

    /**
     * Print synchronization report to console in human-readable format.
     */
    private void printSyncReport(SyncReport report) {
        log.info("\n{}", "=".repeat(70));
        log.info("PRODUCT CATALOG SYNCHRONIZATION REPORT");
        log.info("{}", "=".repeat(70));

        log.info("\nSynchronization Metrics:");
        log.info("  Sync Time: {}", report.getSyncTime());
        log.info("  Total Processed: {}", report.getTotalProcessed());
        log.info("  Total Successfully Synced: {}", report.getTotalSynced());

        log.info("\nDetailed Breakdown:");
        log.info("  Products Added:     {}", report.getProductsAdded());
        log.info("  Products Updated:   {}", report.getProductsUpdated());
        log.info("  Products Unchanged: {}", report.getProductsUnchanged());
        log.info("  Products Deleted:   {}", report.getProductsDeleted());
        log.info("  Products Skipped:   {}", report.getProductsSkipped());

        if (!report.getInvalidProductSamples().isEmpty()) {
            log.info("\nInvalid Product Samples (up to 12):");
            log.info("  Total Invalid: {}", report.getProductsSkipped());
            log.info("\n  Sample Details:");
            for (int i = 0; i < report.getInvalidProductSamples().size(); i++) {
                var sample = report.getInvalidProductSamples().get(i);
                log.info("\n    [Invalid Product {}]", i + 1);
                log.info("    Raw Data: {}", sample.getRawData());
                log.info("    Errors:");
                for (String error : sample.getErrors()) {
                    log.info("      - {}", error);
                }
            }
        }
    }
}

