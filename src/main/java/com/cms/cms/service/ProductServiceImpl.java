package com.cms.cms.service;

import com.cms.cms.model.Product;
import com.cms.cms.model.NewOrg;
import com.cms.cms.Repository.ProductRepository;
import com.cms.cms.Repository.NewOrgRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the ProductService interface with Redis caching
 */
@Service
public class ProductServiceImpl implements ProductService {
    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    // Redis cache keys
    private static final String PRODUCTS_CACHE_KEY = "products:";
    private static final String ALL_AVAILABLE_PRODUCTS_KEY = "products:all:available";
    private static final String ORG_PRODUCTS_KEY = "products:org:";
    private static final String PRODUCTS_NOT_IN_ORG_KEY = "products:not_in_org:";
    private static final long CACHE_TTL = 3600; // 1 hour in seconds

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NewOrgRepository orgRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Cacheable(value = "products", key = "'allAvailable'")
    public List<Product> getAllAvailableProducts() {
        logger.info("Fetching all available products");

        // Try to get from Redis first
        String cacheKey = ALL_AVAILABLE_PRODUCTS_KEY;
        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            try {
                List<Product> cachedProducts = convertToProductList(cachedResult);
                logger.info("Retrieved {} products from Redis cache", cachedProducts.size());
                return cachedProducts;
            } catch (Exception e) {
                logger.warn("Failed to convert cached products, removing corrupted cache: {}", e.getMessage());
                redisTemplate.delete(cacheKey);
            }
        }

        // If not in cache, fetch from database
        List<Product> products = productRepository.findByIsAvailableTrue();

        // Store in Redis with TTL
        redisTemplate.opsForValue().set(cacheKey, products, CACHE_TTL, TimeUnit.SECONDS);
        logger.info("Cached {} available products in Redis", products.size());

        return products;
    }

    @Override
    public List<Product> getProductsForOrganization(Long orgId) {
        logger.info("Fetching products for organization with id: {}", orgId);

        String cacheKey = ORG_PRODUCTS_KEY + orgId;
        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            try {
                // Debug logging
                logger.debug("Cached result type: {}", cachedResult.getClass().getName());

                List<Product> cachedProducts = convertToProductList(cachedResult);
                logger.info("Retrieved {} products for org {} from Redis cache", cachedProducts.size(), orgId);
                return cachedProducts;
            } catch (Exception e) {
                logger.warn("Failed to convert cached products, removing corrupted cache: {}", e.getMessage());
                redisTemplate.delete(cacheKey); // Remove corrupted cache
            }
        }

        // Fetch from database
        List<Product> products = productRepository.findProductsByOrganizationId(orgId);

        // Cache the results
        try {
            redisTemplate.opsForValue().set(cacheKey, products, CACHE_TTL, TimeUnit.SECONDS);
            logger.info("Cached {} products for org {} in Redis", products.size(), orgId);
        } catch (Exception e) {
            logger.warn("Failed to cache products: {}", e.getMessage());
        }

        return products;
    }

    @Override
    public List<Product> getProductsNotInOrganization(Long orgId) {
        logger.info("Fetching products not assigned to organization with id: {}", orgId);

        String cacheKey = PRODUCTS_NOT_IN_ORG_KEY + orgId;
        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            try {
                List<Product> cachedProducts = convertToProductList(cachedResult);
                logger.info("Retrieved {} products not in org {} from Redis cache", cachedProducts.size(), orgId);
                return cachedProducts;
            } catch (Exception e) {
                logger.warn("Failed to convert cached products, removing corrupted cache: {}", e.getMessage());
                redisTemplate.delete(cacheKey);
            }
        }

        List<Product> products = productRepository.findProductsNotInOrganization(orgId);
        redisTemplate.opsForValue().set(cacheKey, products, CACHE_TTL, TimeUnit.SECONDS);
        logger.info("Cached {} products not in org {} in Redis", products.size(), orgId);

        return products;
    }

    @Override
    public List<Product> searchProductsNotInOrganization(String searchTerm, Long orgId) {
        logger.info("Searching products not assigned to organization with id: {} and search term: {}", orgId, searchTerm);
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getProductsNotInOrganization(orgId);
        }

        // For search operations, we'll cache with search term in key
        String cacheKey = PRODUCTS_NOT_IN_ORG_KEY + orgId + ":search:" + searchTerm.toLowerCase();
        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            try {
                List<Product> cachedProducts = convertToProductList(cachedResult);
                logger.info("Retrieved search results from Redis cache");
                return cachedProducts;
            } catch (Exception e) {
                logger.warn("Failed to convert cached search results, removing corrupted cache: {}", e.getMessage());
                redisTemplate.delete(cacheKey);
            }
        }

        List<Product> products = productRepository.searchProductsNotInOrganization(searchTerm, orgId);
        redisTemplate.opsForValue().set(cacheKey, products, CACHE_TTL, TimeUnit.SECONDS);

        return products;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", key = "'allAvailable'"),
            @CacheEvict(value = "products", key = "#orgId")
    })
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

        // Initialize collections if null
        if (org.getProducts() == null) {
            org.setProducts(new HashSet<>());
        }

        if (product.getOrganizations() == null) {
            product.setOrganizations(new HashSet<>());
        }

        // Check if product is already assigned to organization
        if (org.getProducts().contains(product)) {
            logger.warn("Product with id: {} is already assigned to organization with id: {}", productId, orgId);
            return; // No need to add it again
        }

        // Use the helper method to add the product to the organization
        org.addProduct(product);

        // Save the organization (the relationship will be saved due to cascading)
        logger.info("Saving organization with product");
        orgRepository.save(org);

        // Clear related Redis cache entries
        clearOrganizationRelatedCache(orgId);
        clearProductRelatedCache(productId);

        logger.info("Product with id: {} added to organization with id: {}", productId, orgId);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", key = "'allAvailable'"),
            @CacheEvict(value = "products", key = "#orgId")
    })
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

        // Clear related Redis cache entries
        clearOrganizationRelatedCache(orgId);
        clearProductRelatedCache(productId);

        logger.info("Product with id: {} removed from organization with id: {}", productId, orgId);
    }

    @Override
    @Cacheable(value = "products", key = "#id")
    public Product getProductById(Long id) {
        logger.info("Fetching product with id: {}", id);

        String cacheKey = PRODUCTS_CACHE_KEY + id;
        Object cachedResult = redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            try {
                if (cachedResult instanceof Product) {
                    logger.info("Retrieved product {} from Redis cache", id);
                    return (Product) cachedResult;
                } else if (cachedResult instanceof LinkedHashMap) {
                    logger.info("Converting cached LinkedHashMap to Product for id: {}", id);
                    ObjectMapper objectMapper = createObjectMapper();
                    Product product = objectMapper.convertValue(cachedResult, Product.class);
                    logger.info("Retrieved and converted product {} from Redis cache", id);
                    return product;
                }
            } catch (Exception e) {
                logger.warn("Failed to convert cached product, removing corrupted cache: {}", e.getMessage());
                redisTemplate.delete(cacheKey);
            }
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // Cache the product
        redisTemplate.opsForValue().set(cacheKey, product, CACHE_TTL, TimeUnit.SECONDS);
        logger.info("Cached product {} in Redis", id);

        return product;
    }

    @Override
    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(Product request) {
        logger.info("Creating new product with name: {}", request.getName());

        Product product = new Product();
        product.setName(request.getName());
        product.setType(request.getType());
        product.setUnitTypes(request.getUnitTypes());
        product.setAvailableBatches(request.getAvailableBatches());

        Product savedProduct = productRepository.save(product);

        // Clear all products cache since we added a new product
        clearAllProductsCache();

        // Cache the new product
        String cacheKey = PRODUCTS_CACHE_KEY + savedProduct.getId();
        redisTemplate.opsForValue().set(cacheKey, savedProduct, CACHE_TTL, TimeUnit.SECONDS);

        logger.info("Created and cached new product with id: {}", savedProduct.getId());
        return savedProduct;
    }

    /**
     * Convert cached result to List<Product>, handling LinkedHashMap conversion
     */
    @SuppressWarnings("unchecked")
    private List<Product> convertToProductList(Object cachedResult) {
        if (cachedResult instanceof List<?>) {
            List<?> rawList = (List<?>) cachedResult;

            if (rawList.isEmpty()) {
                return Collections.emptyList();
            }

            Object firstItem = rawList.get(0);
            logger.debug("First cached item type: {}", firstItem.getClass().getName());

            // If it's already Product objects, return as is
            if (firstItem instanceof Product) {
                return (List<Product>) rawList;
            }

            // If it's LinkedHashMap, convert to Product using ObjectMapper
            if (firstItem instanceof LinkedHashMap) {
                logger.info("Converting LinkedHashMap to Product objects");
                ObjectMapper objectMapper = createObjectMapper();

                List<Product> products = new ArrayList<>();
                for (Object item : rawList) {
                    try {
                        Product product = objectMapper.convertValue(item, Product.class);
                        products.add(product);
                    } catch (Exception e) {
                        logger.error("Failed to convert item to Product: {}", e.getMessage());
                        throw new RuntimeException("Failed to deserialize cached product", e);
                    }
                }
                return products;
            }

            throw new IllegalStateException("Unexpected cached item type: " + firstItem.getClass());
        }

        throw new IllegalStateException("Unexpected cached data type: " + cachedResult.getClass());
    }

    /**
     * Create properly configured ObjectMapper for Product conversion
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        return objectMapper;
    }

    /**
     * Clear all organization-related cache entries
     */
    private void clearOrganizationRelatedCache(Long orgId) {
        String orgProductsKey = ORG_PRODUCTS_KEY + orgId;
        String productsNotInOrgKey = PRODUCTS_NOT_IN_ORG_KEY + orgId;

        redisTemplate.delete(orgProductsKey);
        redisTemplate.delete(productsNotInOrgKey);

        // Clear search cache for this organization (pattern-based deletion)
        String searchPattern = PRODUCTS_NOT_IN_ORG_KEY + orgId + ":search:*";
        redisTemplate.delete(redisTemplate.keys(searchPattern));

        logger.info("Cleared Redis cache for organization: {}", orgId);
    }

    /**
     * Clear product-related cache entries
     */
    private void clearProductRelatedCache(Long productId) {
        String productKey = PRODUCTS_CACHE_KEY + productId;
        redisTemplate.delete(productKey);

        logger.info("Cleared Redis cache for product: {}", productId);
    }

    /**
     * Clear all products cache
     */
    private void clearAllProductsCache() {
        redisTemplate.delete(ALL_AVAILABLE_PRODUCTS_KEY);

        // Clear all product-specific caches
        String productPattern = PRODUCTS_CACHE_KEY + "*";
        redisTemplate.delete(redisTemplate.keys(productPattern));

        // Clear all organization-related caches
        String orgPattern = ORG_PRODUCTS_KEY + "*";
        redisTemplate.delete(redisTemplate.keys(orgPattern));

        String notInOrgPattern = PRODUCTS_NOT_IN_ORG_KEY + "*";
        redisTemplate.delete(redisTemplate.keys(notInOrgPattern));

        logger.info("Cleared all products-related Redis cache");
    }

    /**
     * Manually clear all Redis cache (useful for admin operations)
     */
    public void clearAllCache() {
        clearAllProductsCache();
        logger.info("Manually cleared all Redis cache");
    }

    /**
     * Get cache statistics (useful for monitoring)
     */
    public void logCacheStats() {
        try {
            Long totalKeys = redisTemplate.execute((RedisCallback<Long>) connection -> {
                return connection.dbSize();
            });
            logger.info("Total Redis keys: {}", totalKeys);
        } catch (Exception e) {
            logger.error("Error getting cache stats", e);
        }
    }
}