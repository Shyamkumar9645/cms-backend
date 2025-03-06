package com.cms.cms.Repository;


import com.cms.cms.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByOrgId(Integer orgId);
    Optional<Order> findByIdAndOrgId(Long id, Integer orgId);
}