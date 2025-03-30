package com.cms.cms.service;

import com.cms.cms.Repository.ProductRepository;
import com.cms.cms.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<Product> getAllAvailableProducts() {
        logger.info("Fetching all available products");
        return productRepository.findByIsAvailableTrue();
    }

    @Override
    public List<Product> getAvailableProductsByType(String type) {
        logger.info("Fetching available products by type: {}", type);
        return productRepository.findByTypeAndIsAvailableTrue(type);
    }

    @Override
    public List<Product> getAvailableProductsByUnitType(String unitType) {
        logger.info("Fetching available products by unit type: {}", unitType);
        return productRepository.findByUnitTypeAndAvailable(unitType);
    }

    @Override
    public Product getProductById(Long id) {
        logger.info("Fetching product by ID: {}", id);
        Optional<Product> product = productRepository.findById(id);

        if (product.isPresent() && product.get().getIsAvailable()) {
            return product.get();
        }

        logger.info("Product not found or not available with ID: {}", id);
        return null;
    }
}