package com.cms.cms.service;

import com.cms.cms.model.Product;
import com.cms.cms.model.NewOrg;
import com.cms.cms.Repository.ProductRepository;
import com.cms.cms.Repository.NewOrgRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for product management
 */

/**
 * Implementation of the ProductService interface
 */
@Service
class ProductServiceImpl implements ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NewOrgRepository orgRepository;

    @Override
    public List<Product> getAllAvailableProducts() {
        logger.info("Fetching all available products");
        return productRepository.findByIsAvailableTrue();
    }

    @Override
    public List<Product> getProductsForOrganization(Long orgId) {
        logger.info("Fetching products for organization with id: {}", orgId);
        return productRepository.findProductsByOrganizationId(orgId);
    }

    @Override
    public List<Product> getProductsNotInOrganization(Long orgId) {
        logger.info("Fetching products not assigned to organization with id: {}", orgId);
        return productRepository.findProductsNotInOrganization(orgId);
    }

    @Override
    public List<Product> searchProductsNotInOrganization(String searchTerm, Long orgId) {
        logger.info("Searching products not assigned to organization with id: {} and search term: {}", orgId, searchTerm);
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getProductsNotInOrganization(orgId);
        }
        return productRepository.searchProductsNotInOrganization(searchTerm, orgId);
    }

    @Override
    @Transactional
    public void addProductToOrganization(Long orgId, Long productId) {
        logger.info("Adding product with id: {} to organization with id: {}", productId, orgId);

        Optional<NewOrg> orgOpt = orgRepository.findById(orgId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (orgOpt.isEmpty()) {
            logger.error("Organization not found with id: {}", orgId);
            throw new RuntimeException("Organization not found");
        }

        if (productOpt.isEmpty()) {
            logger.error("Product not found with id: {}", productId);
            throw new RuntimeException("Product not found");
        }

        if (!productOpt.get().getIsAvailable()) {
            logger.error("Product with id: {} is not available", productId);
            throw new RuntimeException("Product is not available");
        }

        NewOrg org = orgOpt.get();
        Product product = productOpt.get();

        // Check if product is already assigned to organization
        if (org.getProducts().contains(product)) {
            logger.warn("Product with id: {} is already assigned to organization with id: {}", productId, orgId);
            return; // No need to add it again
        }

        // Add the product to the organization
        org.addProduct(product);
        orgRepository.save(org);

        logger.info("Product with id: {} added to organization with id: {}", productId, orgId);
    }

    @Override
    @Transactional
    public void removeProductFromOrganization(Long orgId, Long productId) {
        logger.info("Removing product with id: {} from organization with id: {}", productId, orgId);

        Optional<NewOrg> orgOpt = orgRepository.findById(orgId);
        Optional<Product> productOpt = productRepository.findById(productId);

        if (orgOpt.isEmpty()) {
            logger.error("Organization not found with id: {}", orgId);
            throw new RuntimeException("Organization not found");
        }

        if (productOpt.isEmpty()) {
            logger.error("Product not found with id: {}", productId);
            throw new RuntimeException("Product not found");
        }

        NewOrg org = orgOpt.get();
        Product product = productOpt.get();

        // Check if product is assigned to organization
        if (!org.getProducts().contains(product)) {
            logger.warn("Product with id: {} is not assigned to organization with id: {}", productId, orgId);
            return; // No need to remove it
        }

        // Remove the product from the organization
        org.removeProduct(product);
        orgRepository.save(org);

        logger.info("Product with id: {} removed from organization with id: {}", productId, orgId);
    }

    @Override
    public Product getProductById(Long id) {
        logger.info("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
    }
}