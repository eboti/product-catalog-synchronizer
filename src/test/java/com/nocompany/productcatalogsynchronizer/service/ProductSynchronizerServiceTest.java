package com.nocompany.productcatalogsynchronizer.service;

import com.nocompany.productcatalogsynchronizer.model.InvalidProductSample;
import com.nocompany.productcatalogsynchronizer.model.Product;
import com.nocompany.productcatalogsynchronizer.model.SyncReport;
import com.nocompany.productcatalogsynchronizer.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ProductSynchronizerService.
 * Tests full synchronization cycle with embedded H2 database.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Product Synchronizer Service Integration Tests")
class ProductSynchronizerServiceTest {

    @Autowired
    private ProductSynchronizerService synchronizerService;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Should sync valid products from CSV stream")
    void testSyncValidProducts() throws Exception {
        String csvData = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                        "1,Product 1,Desc 1,in stock,new,100 USD,90 USD,https://example.com/1,Brand A,https://example.com/1.jpg,all,543542\n" +
                        "2,Product 2,Desc 2,out of stock,used,200 USD,,https://example.com/2,Brand B,https://example.com/2.jpg,,543542\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        boolean success = synchronizerService.syncFromStream(inputStream);

        assertTrue(success, "Synchronization should succeed");
        assertEquals(2, productRepository.count(), "Should have 2 products in database");
    }

    @Test
    @DisplayName("Should skip invalid products and report them")
    void testSkipInvalidProducts() throws Exception {
        String csvData = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                        "1,Product 1,Desc 1,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n" +
                        ",Product 2,Desc 2,in stock,new,200 USD,,https://example.com/2,Brand B,,,543542\n" +
                        "3,Product 3,Desc 3,INVALID,new,300 USD,,https://example.com/3,Brand C,,,543542\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        boolean success = synchronizerService.syncFromStream(inputStream);

        assertTrue(success);
        assertEquals(1, productRepository.count(), "Should have 1 valid product");

        SyncReport report = synchronizerService.getCurrentReport();
        assertEquals(1, report.getProductsAdded(), "Should have added 1 product");
        assertEquals(2, report.getProductsSkipped(), "Should have skipped 2 invalid products");
        assertFalse(report.getInvalidProductSamples().isEmpty(), "Should have invalid product samples");
    }

    @Test
    @DisplayName("Should detect product updates")
    void testDetectProductUpdates() throws Exception {
        // First sync
        String csvData1 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                         "1,Product 1,Original Description,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n";

        InputStream inputStream1 = new ByteArrayInputStream(csvData1.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream1);

        Product product = productRepository.findByProductId("1").orElse(null);
        assertNotNull(product);
        assertEquals("Original Description", product.getDescription());

        // Second sync with updated data
        String csvData2 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                         "1,Product 1,Updated Description,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n";

        InputStream inputStream2 = new ByteArrayInputStream(csvData2.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream2);

        Product updated = productRepository.findByProductId("1").orElse(null);
        assertNotNull(updated);
        assertEquals("Updated Description", updated.getDescription());

        SyncReport report = synchronizerService.getCurrentReport();
        assertEquals(1, report.getProductsUpdated(), "Should have updated 1 product");
    }

    @Test
    @DisplayName("Should implement full-load approach - delete old products")
    void testFullLoadApproach() throws Exception {
        // First sync with 3 products
        String csvData1 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                         "1,Product 1,Desc,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n" +
                         "2,Product 2,Desc,in stock,new,200 USD,,https://example.com/2,Brand B,https://example.com/2.jpg,,543542\n" +
                         "3,Product 3,Desc,in stock,new,300 USD,,https://example.com/3,Brand C,https://example.com/3.jpg,,543542\n";

        InputStream inputStream1 = new ByteArrayInputStream(csvData1.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream1);
        assertEquals(3, productRepository.count(), "Should have 3 products after first sync");

        // Second sync with only 2 products (product 1 and 2, not 3)
        String csvData2 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                         "1,Product 1,Desc,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n" +
                         "2,Product 2,Desc,in stock,new,200 USD,,https://example.com/2,Brand B,https://example.com/2.jpg,,543542\n";

        InputStream inputStream2 = new ByteArrayInputStream(csvData2.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream2);

        assertEquals(2, productRepository.count(), "Should have deleted product 3 (full-load approach)");
        assertFalse(productRepository.existsByProductId("3"), "Product 3 should be deleted");

        SyncReport report = synchronizerService.getCurrentReport();
        assertEquals(1, report.getProductsDeleted(), "Should have deleted 1 product");
    }

    @Test
    @DisplayName("Should track unchanged products")
    void testTrackUnchangedProducts() throws Exception {
        // First sync
        String csvData1 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                         "1,Product 1,Desc 1,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n";

        InputStream inputStream1 = new ByteArrayInputStream(csvData1.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream1);

        // Second sync with same data (unchanged)
        String csvData2 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                         "1,Product 1,Desc 1,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n";

        InputStream inputStream2 = new ByteArrayInputStream(csvData2.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream2);

        SyncReport report = synchronizerService.getCurrentReport();
        assertEquals(1, report.getProductsUnchanged(), "Should track unchanged products");
    }

    @Test
    @DisplayName("Should sync multiple files in order")
    void testSyncMultipleFiles() throws Exception {
        String csvData1 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                         "1,Product 1,Desc,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n";

        String csvData2 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                          "1,Product 1,Desc,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n" +
                          "2,Product 2,Desc,in stock,new,200 USD,,https://example.com/2,Brand B,https://example.com/2.jpg,,543542\n";

        List<java.io.InputStream> streams = List.of(
            new ByteArrayInputStream(csvData1.getBytes(StandardCharsets.UTF_8)),
            new ByteArrayInputStream(csvData2.getBytes(StandardCharsets.UTF_8))
        );

        SyncReport report = synchronizerService.syncFromStreams(streams);

        // Full-load approach: only products from latest file remain, so we have 2
        assertEquals(2, productRepository.count(), "Should have products from second file");
        // First sync: 1 added, Second sync: 1 unchanged + 1 added (file 2 only has 2 products)
        assertEquals(2, report.getProductsAdded(), "Should report first sync added 1 + second sync added 1");
    }

    @Test
    @DisplayName("Should preserve createdAt timestamp across syncs")
    void testPreserveCreatedAtTimestamp() throws Exception {
        // First sync
        String csvData1 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                          "1,Product 1,Original,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n";

        InputStream inputStream1 = new ByteArrayInputStream(csvData1.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream1);

        Product product1 = productRepository.findByProductId("1").orElse(null);
        assertNotNull(product1);
        var createdAt1 = product1.getCreatedAt();

        // Wait a moment
        Thread.sleep(100);

        // Second sync with updated data
        String csvData2 = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                          "1,Product 1,Updated,in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n";

        InputStream inputStream2 = new ByteArrayInputStream(csvData2.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream2);

        Product product2 = productRepository.findByProductId("1").orElse(null);
        assertNotNull(product2);
        var createdAt2 = product2.getCreatedAt();

        // createdAt should be the same (not updated)
        assertEquals(createdAt1, createdAt2, "createdAt should remain unchanged across syncs");
        // lastUpdatedAt should be different
        assertTrue(product2.getLastUpdatedAt().isAfter(createdAt1), "lastUpdatedAt should be more recent");
    }

    @Test
    @DisplayName("Should handle special characters and HTML entities in data")
    void testHandleSpecialCharacters() throws Exception {
        String csvData = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                        "1,Product &aacute;&eacute;,\"Desc with, comma and &amp; entities\",in stock,new,100 USD,,https://example.com/1,Brand A,https://example.com/1.jpg,,543542\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        synchronizerService.syncFromStream(inputStream);

        Product product = productRepository.findByProductId("1").orElse(null);
        assertNotNull(product);
        assertEquals("Product &aacute;&eacute;", product.getTitle());
        assertTrue(product.getDescription().contains("&amp;"));
    }

    @Test
    @DisplayName("Should generate comprehensive sync report")
    void testGenerateSyncReport() throws Exception {
        // Note: CSV must have all columns present, even if empty (using CSV escaping where needed)
        String csvData = "id,title,description,availability,condition,price,sale_price,link,brand,image_link,age_group,google_product_category\n" +
                        "\"VALID\",\"Valid Product\",\"Desc\",\"in stock\",\"new\",\"100 USD\",\"\",\"https://example.com/1\",\"Brand A\",\"https://example.com/1.jpg\",\"\",\"543542\"\n" +
                        "\"INVALID\",\"Missing required fields\",\"\",\"\",\"\",\"\",\"\",\"https://example.com/2\",\"\",\"https://example.com/2.jpg\",\"\",\"543542\"\n";

        InputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        SyncReport report = synchronizerService.syncFromStreams(List.of(inputStream));

        assertNotNull(report);
        assertNotNull(report.getSyncTime());
        assertEquals(1, report.getProductsAdded());
        assertEquals(1, report.getProductsSkipped());
        assertEquals(2, report.getTotalProcessed());

        // Check invalid product samples
        assertFalse(report.getInvalidProductSamples().isEmpty());
        InvalidProductSample sample = report.getInvalidProductSamples().get(0);
        assertFalse(sample.getErrors().isEmpty());
    }
}


