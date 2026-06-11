package com.nocompany.productcatalogsynchronizer.service;

import java.util.List;
import java.util.Map;

/**
 * Interface for validating products based on Google Product Data Feed specification.
 * Validates required fields and basic format requirements.
 */
public interface ProductValidatorInterface {

    /**
     * Validates a product mapping against requirements.
     *
     * @param productData map of field names to values from CSV row
     * @return list of validation errors (empty if valid)
     */
    List<String> validate(Map<String, String> productData);
}
