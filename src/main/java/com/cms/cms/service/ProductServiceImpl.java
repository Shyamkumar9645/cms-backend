package com.cms.cms.service;

import com.cms.cms.Repository.ProductRepository;
import com.cms.cms.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Cacheable(value = "products", key = "'allAvailable'")
    public List<Product> getAllAvailableProducts() {
        logger.info("Fetching all available products from database");
        return productRepository.findByIsAvailableTrue();
    }

    @Override
    @Cacheable(value = "products", key = "'byType:' + #type")
    public List<Product> getAvailableProductsByType(String type) {
        logger.info("Fetching available products by type: {} from database", type);
        return productRepository.findByTypeAndIsAvailableTrue(type);
    }

    @Override
    @Cacheable(value = "products", key = "'byUnitType:' + #unitType")
    public List<Product> getAvailableProductsByUnitType(String unitType) {
        logger.info("Fetching available products by unit type: {} from database", unitType);
        return productRepository.findByUnitTypeAndAvailable(unitType);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public Product getProductById(Long id) {
        logger.info("Fetching product by ID: {} from database", id);
        Optional<Product> product = productRepository.findById(id);

        if (product.isPresent() && product.get().getIsAvailable()) {
            return product.get();
        }

        logger.info("Product not found or not available with ID: {}", id);
        return null;
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public void clearProductCache() {
        logger.info("Clearing entire product cache");
        // This method will clear all entries in the "products" cache
    }

    @Override
    @CacheEvict(value = "products", key = "#id")
    public void clearProductCacheById(Long id) {
        logger.info("Clearing product cache for ID: {}", id);
        // This method will clear the cache entry for the specified product ID
    }
}