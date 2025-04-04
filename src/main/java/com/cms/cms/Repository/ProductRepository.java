package com.cms.cms.Repository;

import com.cms.cms.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find all available products
    List<Product> findByIsAvailableTrue();

    // Find products by type and availability
    List<Product> findByTypeAndIsAvailableTrue(String type);

    // Find products by name containing a search term and availability
    List<Product> findByNameContainingIgnoreCaseAndIsAvailableTrue(String name);

    // Find products that are available to a specific organization
    @Query("SELECT p FROM Product p JOIN p.organizations o WHERE o.id = :orgId")
    List<Product> findProductsByOrganizationId(@Param("orgId") Long orgId);

    // Find products that are NOT available to a specific organization
    @Query("SELECT p FROM Product p WHERE p.isAvailable = true AND p.id NOT IN " +
            "(SELECT p2.id FROM Product p2 JOIN p2.organizations o WHERE o.id = :orgId)")
    List<Product> findProductsNotInOrganization(@Param("orgId") Long orgId);

    // Search products by name or type that are NOT available to a specific organization
    @Query("SELECT p FROM Product p WHERE p.isAvailable = true AND " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.type) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "p.id NOT IN (SELECT p2.id FROM Product p2 JOIN p2.organizations o WHERE o.id = :orgId)")
    List<Product> searchProductsNotInOrganization(@Param("searchTerm") String searchTerm, @Param("orgId") Long orgId);
}