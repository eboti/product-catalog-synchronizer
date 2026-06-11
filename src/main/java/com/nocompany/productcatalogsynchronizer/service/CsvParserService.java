package com.nocompany.productcatalogsynchronizer.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service for parsing CSV/TSV product data feeds.
 * Handles both comma-separated and tab-separated values.
 */
@Service
public class CsvParserService implements CsvParser {

    /**
     * Parse a CSV/TSV file and return list of product maps.
     * Automatically detects whether the file is tab-delimited or comma-delimited.
     *
     * @param inputStream the input stream of the CSV/TSV file
     * @return list of maps where each map represents a product with field names as keys
     * @throws IOException if there's an error reading the file
     */
    public List<Map<String, String>> parseProducts(InputStream inputStream) throws IOException {
        List<Map<String, String>> products = new ArrayList<>();

        // Read entire stream into a string to allow detection and re-parsing
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        String content = result.toString(StandardCharsets.UTF_8);

        if (content.trim().isEmpty()) {
            return products;
        }

        // Detect delimiter from the content
        char delimiter = detectDelimiter(content);

        // Parse using detected delimiter
        ByteArrayInputStream bais = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(bais, StandardCharsets.UTF_8)
        );

        CSVFormat format = CSVFormat.DEFAULT
                .withDelimiter(delimiter)
                .withEscape('\\')                // accept backslash-escaped quotes like \"
                .withQuote('"')                 // explicit quote char (default is '"', but explicit helps readability)
                .withFirstRecordAsHeader()
                .withAllowMissingColumnNames()
                .withTrim();

        @SuppressWarnings("resource")
        CSVParser parser = new CSVParser(reader, format);

        for (CSVRecord record : parser) {
            Map<String, String> productMap = new LinkedHashMap<>();

            // Get headers from parser
            Set<String> headers = parser.getHeaderMap().keySet();
            for (String header : headers) {
                String value = record.get(header);
                productMap.put(header, value != null ? value : "");
            }

            // Only add non-empty rows (skip rows with all empty values)
            if (!isEmptyRow(productMap)) {
                products.add(productMap);
            }
        }

        parser.close();

        return products;
    }

    /**
     * Detect the delimiter used in CSV file (tab or comma).
     *
     * @param content the content of the CSV file
     * @return '\t' if tab-delimited, ',' otherwise (CSV is preferred by default)
     */
    private char detectDelimiter(String content) {
        // Get first line
        String firstLine = content.split("\n")[0];

        // Count occurrences of tab and comma
        long tabCount = firstLine.chars().filter(ch -> ch == '\t').count();
        long commaCount = firstLine.chars().filter(ch -> ch == ',').count();

        // If more tabs than commas, assume tab-delimited; otherwise default to comma (CSV)
        if (tabCount > commaCount) {
            return '\t';
        }

        return ',';
    }

    /**
     * Check if a row is empty (all values are empty strings or whitespace).
     */
    private boolean isEmptyRow(Map<String, String> row) {
        return row.values().stream()
            .allMatch(val -> val == null || val.trim().isEmpty());
    }

    /**
     * Parse a CSV/TSV file from a file path.
     *
     * @param filePath the path to the CSV/TSV file
     * @return list of product maps
     * @throws IOException if there's an error reading the file
     */
    public List<Map<String, String>> parseProductsFromFile(String filePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return parseProducts(inputStream);
        }
    }
}

