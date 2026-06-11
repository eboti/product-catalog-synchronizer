package com.nocompany.productcatalogsynchronizer.util;

import com.nocompany.productcatalogsynchronizer.service.CsvParserService;
import com.nocompany.productcatalogsynchronizer.service.ProductValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small CLI utility to validate product files and print invalid rows.
 *
 * Usage: run this class from your IDE or via `mvn exec:java -Dexec.mainClass=... -Dexec.args="path1 path2"`
 */
public class ProductValidationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductValidationRunner.class);

    public static void main(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            log.error("Usage: ProductValidationRunner <file1> [file2 ...]");
            System.exit(2);
        }

        CsvParserService parser = new CsvParserService();
        ProductValidator validator = new ProductValidator();

        for (String p : args) {
            Path path = Path.of(p);
            if (!Files.exists(path)) {
                log.warn("File not found: {}", p);
                continue;
            }

            log.info("Validating file: {}", p);
            List<Map<String, String>> products;
            try {
                products = parser.parseProductsFromFile(p);
            } catch (IOException e) {
                log.error("Failed to parse file {}: {}", p, e.getMessage(), e);
                continue;
            }

            int row = 1; // CSV parser returns records without header
            int invalidCount = 0;
            for (Map<String, String> product : products) {
                List<String> errors = validator.validate(product);
                if (!errors.isEmpty()) {
                    invalidCount++;
                    log.info("--- Row {} ---", row);
                    log.info("{}", product);
                    for (String err : errors) {
                        log.info("  - {}", err);
                    }
                }
                row++;
            }

            log.info("Finished validating {}. Invalid rows: {}, total rows: {}", p, invalidCount, products.size());
        }
    }
}

