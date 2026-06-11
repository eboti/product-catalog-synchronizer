package com.nocompany.productcatalogsynchronizer.service;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Validator for products based on Google Product Data Feed specification.
 * Validates required fields and basic format requirements.
 */
@Service
public class ProductValidator implements ProductValidatorInterface {

    private static final List<String> VALID_CURRENCIES = List.of(
            "USD",
            "HUF",
            "EUR"
    );

    // Valid availability values per Google specification.
    // Accept both underscore and space variants (e.g. in_stock or in stock).
    private static final List<String> VALID_AVAILABILITY_VALUES = List.of(
        "in_stock",
        "out_of_stock",
        "preorder",
        "backorder",
        "in stock",
        "out of stock"
    );

    // Valid condition values per Google specification
    private static final List<String> VALID_CONDITION_VALUES = List.of(
        "new",
        "refurbished",
        "used"
    );

    /**
     * Validates a product mapping against requirements.
     *
     * @param productData map of field names to values from CSV row
     * @return list of validation errors (empty if valid)
     */
    public List<String> validate(java.util.Map<String, String> productData) {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (!hasValue(productData, "id")) {
            errors.add("Missing required field: id");
        } else {
            String productId = productData.get("id").trim();
            if (productId.isEmpty()) {
                errors.add("Field 'id' cannot be empty");
            } else if (productId.length() > 50) {
                // Validation-criterias.md specifies max 50 characters for id
                errors.add("Field 'id' exceeds maximum length of 50 characters");
            }
        }

        if (!hasValue(productData, "title")) {
            errors.add("Missing required field: title");
        } else {
            String title = productData.get("title").trim();
            if (title.isEmpty()) {
                errors.add("Field 'title' cannot be empty");
            } else if (title.length() > 150) {
                // Validation-criterias.md specifies max 150 characters for title
                errors.add("Field 'title' exceeds maximum length of 150 characters");
            }
        }

        // Description is required (either description or structured_description)
        if (!hasValue(productData, "description") && !hasValue(productData, "structured_description")) {
            errors.add("Missing required field: description or structured_description");
        } else if (hasValue(productData, "description")) {
            String desc = productData.get("description").trim();
            if (desc.length() > 5000) {
                errors.add("Field 'description' exceeds maximum length of 5000 characters");
            }
        }

        if (!hasValue(productData, "availability")) {
            errors.add("Missing required field: availability");
        } else {
            String availability = productData.get("availability");
            if (!VALID_AVAILABILITY_VALUES.contains(availability)) {
                errors.add(String.format(
                    "Invalid availability value '%s'. Must be one of: %s",
                    productData.get("availability"), String.join(", ", VALID_AVAILABILITY_VALUES)
                ));
            }
        }

        if (!hasValue(productData, "condition")) {
            errors.add("Missing required field: condition");
        } else {
            String condition = productData.get("condition").trim().toLowerCase();
            if (!VALID_CONDITION_VALUES.contains(condition)) {
                errors.add(String.format(
                    "Invalid condition value '%s'. Must be one of: %s",
                    condition, String.join(", ", VALID_CONDITION_VALUES)
                ));
            }
        }

        if (!hasValue(productData, "price")) {
            errors.add("Missing required field: price");
        } else {
            String price = productData.get("price").trim();
            if (!isValidPrice(price)) {
                errors.add(String.format("Invalid price format: '%s'. Expected amount and currency (e.g., '100.00 USD' or '100 HUF')", price));
            }
        }

        if (!hasValue(productData, "link")) {
            errors.add("Missing required field: link");
        } else {
            String link = productData.get("link").trim();
            if (!isValidUrl(link)) {
                errors.add(String.format("Invalid link format: '%s'. Must be a valid URL starting with http/https", link));
            }
        }

        if (!hasValue(productData, "brand")) {
            errors.add("Missing required field: brand");
        } else {
            String brand = productData.get("brand").trim();
            if (brand.isEmpty()) {
                errors.add("Field 'brand' cannot be empty");
            } else if (brand.length() > 70) {
                // Validation-criterias.md specifies max 70 characters for brand
                errors.add("Field 'brand' exceeds maximum length of 70 characters");
            }
        }

        // Validate optional fields if present
        if (hasValue(productData, "sale_price")) {
            String salePrice = productData.get("sale_price").trim();
            if (!salePrice.isEmpty() && !isValidPrice(salePrice)) {
                errors.add(String.format("Invalid sale_price format: '%s'. Expected amount and currency (e.g., '100.00 USD' or '100 HUF')", salePrice));
            }
        }

        // image_link is required per specification
        if (!hasValue(productData, "image_link")) {
            errors.add("Missing required field: image_link");
        } else {
            String imageLink = productData.get("image_link").trim();
            if (!isValidUrl(imageLink)) {
                errors.add(String.format("Invalid image_link format: '%s'. Must be a valid URL starting with http/https", imageLink));
            } else if (!hasValidImageExtension(imageLink)) {
                errors.add(String.format("Invalid image_link file type: '%s'. Supported extensions: jpg,jpeg,png,webp,gif,bmp,tif,tiff", imageLink));
            }
        }

        return errors;
    }

    /**
     * Check if a field exists and is not null in the data map.
     */
    private boolean hasValue(java.util.Map<String, String> data, String field) {
        return data.containsKey(field) && data.get(field) != null && !data.get(field).trim().isEmpty();
    }

    /**
     * Validate price format (e.g., "100 HUF", "99.99 USD").
     * Expects currency code and numeric amount separated by space.
     */
    private boolean isValidPrice(String price) {
        if (price == null || price.trim().isEmpty()) {
            return false;
        }

        price = price.trim();

        // Split by space to separate amount and currency
        String[] parts = price.split("\\s+");
        if (parts.length < 2) {
            return false;
        }

        // Last part should be currency code (2-3 uppercase letters)

        String currency = parts[parts.length - 1];
        if (!VALID_CURRENCIES.contains(currency)) {
            return false;
        }

        // Everything else should be a valid number
        String amount = String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1));
        // normalize amount: remove spaces, allow comma as decimal or thousands separators
        String normalized = amount.replace("\u00A0", "").trim();
        // Remove common thousand separators (',' or ' ')
        normalized = normalized.replaceAll("[, ]", "");
        // Replace comma decimal separator with dot if present
        normalized = normalized.replace(',', '.');
        try {
            Double.parseDouble(normalized);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate URL format - basic check for http/https protocol.
     */
    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        url = url.trim();
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasValidImageExtension(String url) {
        String lower = url.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
            || lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".tif") || lower.endsWith(".tiff");
    }

    private boolean isValidIso8601Date(String date) {
        if (date == null || date.trim().isEmpty()) {
            return false;
        }
        // Basic ISO8601 validation using java.time parse
        try {
            java.time.OffsetDateTime.parse(date);
            return true;
        } catch (Exception e) {
            // try without offset
            try {
                java.time.LocalDateTime.parse(date);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }
}

