package com.nocompany.productcatalogsynchronizer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProductValidator.
 * Tests validation of product data according to Google Product Data Feed specification.
 */
@DisplayName("Product Validator Tests")
class ProductValidatorTest {

    private ProductValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProductValidator();
    }

    @Test
    @DisplayName("Should validate a correct product with all required fields")
    void testValidateCorrectProduct() {
        Map<String, String> product = createValidProduct();
        List<String> errors = validator.validate(product);
        assertTrue(errors.isEmpty(), "Valid product should have no errors");
    }

    @Test
    @DisplayName("Should fail when id is missing")
    void testMissingId() {
        Map<String, String> product = createValidProduct();
        product.remove("id");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("id")), "Should error on missing id");
    }

    @Test
    @DisplayName("Should fail when id is empty")
    void testEmptyId() {
        Map<String, String> product = createValidProduct();
        product.put("id", "  ");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("id")), "Should error on empty id");
    }

    @Test
    @DisplayName("Should fail when title is missing")
    void testMissingTitle() {
        Map<String, String> product = createValidProduct();
        product.remove("title");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("title")), "Should error on missing title");
    }

    @Test
    @DisplayName("Should fail when availability is invalid")
    void testInvalidAvailability() {
        Map<String, String> product = createValidProduct();
        product.put("availability", "invalid");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("availability")), "Should error on invalid availability");
    }

    @Test
    @DisplayName("Should accept valid availability values")
    void testValidAvailability() {
        Map<String, String> product = createValidProduct();

        for (String availability : List.of("in stock", "out of stock", "preorder")) {
            product.put("availability", availability);
            List<String> errors = validator.validate(product);
            assertFalse(errors.stream().anyMatch(e -> e.contains("availability")),
                "Should accept validity: " + availability);
        }
    }

    @Test
    @DisplayName("Should fail when condition is invalid")
    void testInvalidCondition() {
        Map<String, String> product = createValidProduct();
        product.put("condition", "broken");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("condition")), "Should error on invalid condition");
    }

    @Test
    @DisplayName("Should accept valid condition values")
    void testValidCondition() {
        Map<String, String> product = createValidProduct();

        for (String condition : List.of("new", "refurbished", "used")) {
            product.put("condition", condition);
            List<String> errors = validator.validate(product);
            assertFalse(errors.stream().anyMatch(e -> e.contains("condition")),
                "Should accept condition: " + condition);
        }
    }

    @Test
    @DisplayName("Should fail when price is invalid")
    void testInvalidPrice() {
        Map<String, String> product = createValidProduct();
        product.put("price", "invalid");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("price")), "Should error on invalid price");
    }

    @Test
    @DisplayName("Should accept valid price formats")
    void testValidPrice() {
        Map<String, String> product = createValidProduct();

        for (String price : List.of("100 USD", "99.99 HUF", "1000.00 EUR")) {
            product.put("price", price);
            List<String> errors = validator.validate(product);
            assertFalse(errors.stream().anyMatch(e -> e.contains("price")),
                "Should accept price: " + price);
        }
    }

    @Test
    @DisplayName("Should reject unsupported currency codes (e.g., GBP)")
    void testRejectUnsupportedCurrency() {
        Map<String, String> product = createValidProduct();
        product.put("price", "50 GBP");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("price")), "Should error on unsupported currency GBP");
    }

    @Test
    @DisplayName("Should fail when link is invalid")
    void testInvalidLink() {
        Map<String, String> product = createValidProduct();
        product.put("link", "not-a-url");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("link")), "Should error on invalid link");
    }

    @Test
    @DisplayName("Should accept valid URLs in link field")
    void testValidLink() {
        Map<String, String> product = createValidProduct();

        for (String link : List.of("http://example.com", "https://example.com/path")) {
            product.put("link", link);
            List<String> errors = validator.validate(product);
            assertFalse(errors.stream().anyMatch(e -> e.contains("link")),
                "Should accept link: " + link);
        }
    }

    @Test
    @DisplayName("Should fail when brand is missing")
    void testMissingBrand() {
        Map<String, String> product = createValidProduct();
        product.remove("brand");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("brand")), "Should error on missing brand");
    }

    @Test
    @DisplayName("Should accept empty optional fields")
    void testOptionalFields() {
        Map<String, String> product = createValidProduct();
        // Keep description and image_link (required per spec). Only other optional fields may be empty.
        product.put("age_group", "");
        List<String> errors = validator.validate(product);
        assertTrue(errors.isEmpty(), "Should accept empty optional fields");
    }

    @Test
    @DisplayName("Should fail when image_link has invalid URL")
    void testInvalidImageLink() {
        Map<String, String> product = createValidProduct();
        product.put("image_link", "not-a-valid-url");
        List<String> errors = validator.validate(product);
        assertTrue(errors.stream().anyMatch(e -> e.contains("image_link")), "Should error on invalid image_link");
    }

    private Map<String, String> createValidProduct() {
        Map<String, String> product = new HashMap<>();
        product.put("id", "9789633764305");
        product.put("title", "Valid Product Title");
        product.put("description", "A valid product description");
        product.put("availability", "in stock");
        product.put("condition", "new");
        product.put("price", "100 HUF");
        product.put("sale_price", "80 HUF");
        product.put("link", "https://example.com/product");
        product.put("brand", "Example Brand");
        product.put("image_link", "https://example.com/image.jpg");
        product.put("age_group", "");
        product.put("google_product_category", "543542");
        return product;
    }
}

