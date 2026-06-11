package com.nocompany.productcatalogsynchronizer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Sample of an invalid product with its validation errors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvalidProductSample implements Serializable {

    /** The raw data of the invalid product */
    private String rawData;

    /** List of validation error messages explaining what is wrong */
    private List<String> errors;
}

