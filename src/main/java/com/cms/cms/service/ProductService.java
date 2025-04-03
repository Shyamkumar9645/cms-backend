package com.cms.cms.service;

import com.cms.cms.model.Product;
import java.util.List;

public interface ProductService {
    /**
     * Get all available products
     * @return List of available products
     */
    List<Product> getAllAvailableProducts();

    /**
     * Get available products by type
     * @param type The product type
     * @return List of available products of the specified type
     */
    List<Product> getAvailableProductsByType(String type);

    /**
     * Get available products by unit type
     * @param unitType The unit type
     * @return List of available products with the specified unit type
     */
    List<Product> getAvailableProductsByUnitType(String unitType);

    /**
     * Get a product by ID
     * @param id The product ID
     * @return The product if found, null otherwise
     */
    Product getProductById(Long id);

    /**
     * Clear the entire product cache
     */
    void clearProductCache();

    /**
     * Clear the cache for a specific product
     * @param id The product ID
     */
    void clearProductCacheById(Long id);
}