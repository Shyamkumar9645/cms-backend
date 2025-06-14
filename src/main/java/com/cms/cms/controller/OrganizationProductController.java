package com.cms.cms.controller;

import com.cms.cms.model.Product;
import com.cms.cms.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API Controller for managing the relationship between organizations and products
 */
@RestController
@RequestMapping("/api")
public class OrganizationProductController {
    private static final Logger logger = LoggerFactory.getLogger(OrganizationProductController.class);

    @Autowired
    private ProductService productService;

    /**
     * Get all products available to an organization
     */
    @GetMapping("/organization/{orgId}/products")
    public ResponseEntity<?> getOrganizationProducts(@PathVariable Long orgId) {
        logger.info("Fetching products for organization: {}", orgId);

        try {
            List<Product> products = productService.getProductsForOrganization(orgId);
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching products for organization: {}", orgId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching products: " + e.getMessage()));
        }
    }

    /**
     * Get all available products (admin endpoint)
     */
    @GetMapping("/admin/products")
    public ResponseEntity<?> getAllProducts() {
        logger.info("Fetching all available products");

        try {
            List<Product> products = productService.getAllAvailableProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching all products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching products: " + e.getMessage()));
        }
    }

    @PostMapping("/admin/products")
    public ResponseEntity<?> createProduct(@RequestBody Product productRequest) {
        logger.info("Creating new product: {}", productRequest.getName());

        try {
            // Delegate to service to save the product
            Product createdProduct = productService.createProduct(productRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Product created successfully");
            response.put("product", createdProduct);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error creating product", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error creating product: " + e.getMessage()));
        }
    }

    /**
     * Get products not assigned to an organization
     */
    @GetMapping("/admin/organization/{orgId}/available-products")
    public ResponseEntity<?> getAvailableProductsForOrg(
            @PathVariable Long orgId,
            @RequestParam(required = false) String search) {

        logger.info("Fetching available products for organization: {} with search: {}", orgId, search);

        try {
            List<Product> products;

            if (search != null && !search.trim().isEmpty()) {
                products = productService.searchProductsNotInOrganization(search, orgId);
            } else {
                products = productService.getProductsNotInOrganization(orgId);
            }

            return ResponseEntity.ok(products);
        } catch (Exception e) {
            logger.error("Error fetching available products for organization: {}", orgId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching available products: " + e.getMessage()));
        }
    }

    /**
     * Add a product to an organization
     */
    @PostMapping("/organization/{orgId}/products")
    public ResponseEntity<?> addProductToOrganization(
            @PathVariable Long orgId,
            @RequestBody Map<String, Long> request) {

        Long productId = request.get("productId");

        if (productId == null) {
            return ResponseEntity.badRequest()
                    .body(createErrorResponse("Product ID is required"));
        }

        logger.info("Adding product: {} to organization: {}", productId, orgId);

        try {
            // Add more detailed logging
            logger.info("Starting transaction to add product to organization");

            productService.addProductToOrganization(orgId, productId);

            logger.info("Successfully added product: {} to organization: {}", productId, orgId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Product added to organization successfully");
            response.put("organizationId", orgId);
            response.put("productId", productId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the full stack trace
            logger.error("Error adding product to organization: {}", e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error adding product: " + e.getMessage()));
        }
    }

    /**
     * Remove a product from an organization
     */
    @DeleteMapping("/organization/{orgId}/products/{productId}")
    public ResponseEntity<?> removeProductFromOrganization(
            @PathVariable Long orgId,
            @PathVariable Long productId) {

        logger.info("Removing product: {} from organization: {}", productId, orgId);

        try {
            productService.removeProductFromOrganization(orgId, productId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Product removed from organization successfully");
            response.put("organizationId", orgId);
            response.put("productId", productId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error removing product from organization", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error removing product: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create standardized error responses
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}