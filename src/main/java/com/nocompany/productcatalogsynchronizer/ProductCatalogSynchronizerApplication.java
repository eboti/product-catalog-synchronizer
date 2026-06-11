package com.nocompany.productcatalogsynchronizer;

import com.nocompany.productcatalogsynchronizer.model.InvalidProductSample;
import com.nocompany.productcatalogsynchronizer.model.SyncReport;
import com.nocompany.productcatalogsynchronizer.service.ProductSynchronizerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class ProductCatalogSynchronizerApplication {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogSynchronizerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ProductCatalogSynchronizerApplication.class, args);
    }

    @Profile("!test")
    @Bean
    CommandLineRunner commandLineRunner(ProductSynchronizerService synchronizerService) {
        return args -> {

            log.info("\n{}", "=".repeat(70));
            log.info("PRODUCT CATALOG SYNCHRONIZER - INTERACTIVE MODE");
            log.info("{}", "=".repeat(70));

            Scanner scanner = new Scanner(System.in);

            log.info("\nEnter file paths to synchronize.");
            log.info("Type 'exit' to quit.\n");

            while (true) {

                log.info("Enter file path: ");
                String filePath = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(filePath)) {
                    log.info("Exiting...");
                    break;
                }

                if (filePath.isEmpty()) {
                    continue;
                }

                if (!Files.exists(Paths.get(filePath))) {
                    log.warn("❌ File not found: {}", filePath);
                    continue;
                }

                try {
                    log.info("\nSynchronizing: {}", filePath);
                    SyncReport report = synchronizerService.syncFromFiles(List.of(filePath));
                    printReport(report);
                } catch (IOException e) {
                    log.error("Synchronization failed: {}", e.getMessage(), e);
                }
            }

            scanner.close();
        };
    }

    private void printReport(SyncReport report) {
        log.info("\n{}", "=".repeat(70));
        log.info("SYNCHRONIZATION REPORT");
        log.info("{}", "=".repeat(70));

        log.info("\nSynchronization Time: {}", report.getSyncTime());

        log.info("\n{}", "-".repeat(70));
        log.info("METRICS SUMMARY");
        log.info("{}", "-".repeat(70));
        log.info("  Products Added:      {}", report.getProductsAdded());
        log.info("  Products Updated:    {}", report.getProductsUpdated());
        log.info("  Products Unchanged:  {}", report.getProductsUnchanged());
        log.info("  Products Deleted:    {}", report.getProductsDeleted());
        log.info("  Products Skipped:    {}", report.getProductsSkipped());
        log.info("{}", "-".repeat(70));
        log.info("  Total Processed:     {}", report.getTotalProcessed());
        log.info("  Total Synced:        {}", report.getTotalSynced());

        if (report.getProductsSkipped() > 0) {
            log.info("\n{}", "-".repeat(70));
            log.info("INVALID PRODUCTS ({} samples)", report.getInvalidProductSamples().size());
            log.info("{}", "-".repeat(70));

            for (int i = 0; i < report.getInvalidProductSamples().size(); i++) {
                InvalidProductSample sample = report.getInvalidProductSamples().get(i);
                log.info("\n[Invalid Product {}]", i + 1);
                log.info("Data: {}", sample.getRawData());
                log.info("Errors:");
                for (String error : sample.getErrors()) {
                    log.info("  • {}", error);
                }
            }
        }

        log.info("\n{}", "=".repeat(70));
        log.info("✓ Synchronization complete!");
        log.info("{}\n", "=".repeat(70));
    }
}