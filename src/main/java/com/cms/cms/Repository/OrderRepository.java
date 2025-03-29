package com.cms.cms.Repository;

import com.cms.cms.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrgId(Integer orgId);
    Optional<Order> findByIdAndOrgId(Long id, Integer orgId);

    // New methods for pending orders
    @Query("SELECT o FROM Order o WHERE o.status = 'Pending' ORDER BY o.date DESC")
    List<Order> findAllPendingOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'Pending'")
    long countPendingOrders();

    Optional<Order> findByIdAndStatus(Long id, String status);
}