package com.cms.cms.Repository;

import com.cms.cms.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find all available products
    List<Product> findByIsAvailableTrue();

    // Find by type and availability
    List<Product> findByTypeAndIsAvailableTrue(String type);

    // Custom query to find products with specific unit type
    @Query("SELECT p FROM Product p JOIN p.unitTypes u WHERE u = ?1 AND p.isAvailable = true")
    List<Product> findByUnitTypeAndAvailable(String unitType);
}