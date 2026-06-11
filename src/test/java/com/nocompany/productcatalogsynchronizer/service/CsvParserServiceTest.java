package com.nocompany.productcatalogsynchronizer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CsvParserService.
 * Tests parsing of CSV and TSV product feeds.
 */
@DisplayName("CSV Parser Service Tests")
class CsvParserServiceTest {

    private CsvParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new CsvParserService();
    }

    @Test
    @DisplayName("Should parse comma-delimited CSV with headers")
    void testParseCommaSeparatedCsv() throws IOException {
        String csvData = "id,title,brand,price\n" +
                        "1,Product 1,Brand A,100 USD\n" +
                        "2,Product 2,Brand B,200 USD\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(2, products.size(), "Should parse 2 products");
        assertEquals("Product 1", products.get(0).get("title"));
        assertEquals("Brand A", products.get(0).get("brand"));
    }

    @Test
    @DisplayName("Should parse tab-delimited TSV data")
    void testParseTabSeparatedTsv() throws IOException {
        String tsvData = "id\ttitle\tbrand\tprice\n" +
                        "1\tProduct 1\tBrand A\t100 USD\n" +
                        "2\tProduct 2\tBrand B\t200 USD\n";

        InputStream inputStream = new ByteArrayInputStream(tsvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(2, products.size(), "Should parse 2 products");
        assertEquals("Product 1", products.get(0).get("title"));
        assertEquals("Brand A", products.get(0).get("brand"));
    }

    @Test
    @DisplayName("Should skip empty rows")
    void testSkipEmptyRows() throws IOException {
        String csvData = "id,title,brand,price\n" +
                        "1,Product 1,Brand A,100 USD\n" +
                        ",,,,\n" +
                        "2,Product 2,Brand B,200 USD\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(2, products.size(), "Should skip empty rows and parse 2 products");
    }

    @Test
    @DisplayName("Should handle quoted fields with commas")
    void testHandleQuotedFields() throws IOException {
        String csvData = "id,title,description\n" +
                        "1,Product 1,\"Description with, comma\"\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(1, products.size());
        assertTrue(products.get(0).get("description").contains("comma"));
    }

    @Test
    @DisplayName("Should handle empty file")
    void testHandleEmptyFile() throws IOException {
        String csvData = "";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(0, products.size(), "Should return empty list for empty file");
    }

    @Test
    @DisplayName("Should handle file with only headers")
    void testHandleHeaderOnly() throws IOException {
        String csvData = "id,title,brand,price\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(0, products.size(), "Should return empty list when no data rows");
    }

    @Test
    @DisplayName("Should handle missing values in fields")
    void testHandleMissingValues() throws IOException {
        String csvData = "id,title,brand,price\n" +
                        "1,Product 1,,100 USD\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(1, products.size());
        assertEquals("1", products.get(0).get("id"));
        assertEquals("Product 1", products.get(0).get("title"));
        assertEquals("", products.get(0).get("brand"));
    }

    @Test
    @DisplayName("Should trim whitespace from values")
    void testTrimWhitespace() throws IOException {
        String csvData = "id,title,brand\n" +
                        "  1  ,  Product 1  ,  Brand A  \n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(1, products.size());
        assertEquals("1", products.get(0).get("id"));
        assertEquals("Product 1", products.get(0).get("title"));
        assertEquals("Brand A", products.get(0).get("brand"));
    }

    @Test
    @DisplayName("Should parse real-world product data format")
    void testParseRealWorldData() throws IOException {
        String csvData = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                        "9789633764305,A szentivánéji álom,\"A ​Szentiv&aacute;n&eacute;ji &aacute;lom\",in stock,new,380 HUF,380 HUF,https://dibook.hu/konyv/a-szentivaneji-alom-1,Fapadoskonyv.hu,https://dibook.hu/storage/books/pr_4179/cover-big.webp,,543542\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<Map<String, String>> products = parserService.parseProducts(inputStream);

        assertEquals(1, products.size());
        assertEquals("9789633764305", products.get(0).get("id"));
        assertEquals("A szentivánéji álom", products.get(0).get("title"));
        assertEquals("in stock", products.get(0).get("availability"));
        assertEquals("https://dibook.hu/konyv/a-szentivaneji-alom-1", products.get(0).get("link"));
    }
}

