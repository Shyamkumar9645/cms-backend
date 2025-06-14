package com.cms.cms.service;

import com.cms.cms.model.Product;
import java.util.List;

public interface ProductService {
    /**
     * Get all available products
     */
    List<Product> getAllAvailableProducts();

    /**
     * Get products available to a specific organization
     */
    List<Product> getProductsForOrganization(Long orgId);

    /**
     * Get all products not assigned to a specific organization
     */
    List<Product> getProductsNotInOrganization(Long orgId);

    /**
     * Search products not assigned to an organization
     */
    List<Product> searchProductsNotInOrganization(String searchTerm, Long orgId);

    /**
     * Add a product to an organization
     */
    void addProductToOrganization(Long orgId, Long productId);

    /**
     * Remove a product from an organization
     */
    void removeProductFromOrganization(Long orgId, Long productId);

    /**
     * Get a product by ID
     */
    Product getProductById(Long id);

      Product  createProduct(Product request) ;

}
