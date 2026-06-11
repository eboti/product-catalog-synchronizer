package com.nocompany.productcatalogsynchronizer.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Interface for parsing CSV/TSV product data feeds.
 */
public interface CsvParser {

    /**
     * Parse a CSV/TSV file and return list of product maps.
     * Automatically detects whether the file is tab-delimited or comma-delimited.
     *
     * @param inputStream the input stream of the CSV/TSV file
     * @return list of maps where each map represents a product with field names as keys
     * @throws IOException if there's an error reading the file
     */
    List<Map<String, String>> parseProducts(InputStream inputStream) throws IOException;

    /**
     * Parse a CSV/TSV file from a file path.
     *
     * @param filePath the path to the CSV/TSV file
     * @return list of product maps
     * @throws IOException if there's an error reading the file
     */
    List<Map<String, String>> parseProductsFromFile(String filePath) throws IOException;
}
