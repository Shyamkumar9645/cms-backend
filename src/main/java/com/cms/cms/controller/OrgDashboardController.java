package com.cms.cms.controller;

import com.cms.cms.model.NewOrg;
import com.cms.cms.model.Order;
import com.cms.cms.service.NewOrgService;
import com.cms.cms.service.OrgOrderService;
import com.cms.cms.service.OrganizationUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/org")
public class OrgDashboardController {

    @Autowired
    private NewOrgService newOrgService;

    @Autowired
    private OrgOrderService orgOrderService;

    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/dashboard/profile")
    public ResponseEntity<?> getOrgProfile() {
        // Get current authenticated organization user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OrganizationUserDetails userDetails = (OrganizationUserDetails) authentication.getPrincipal();

        // Get organization ID
        Integer orgId = userDetails.getOrgId();

        // Fetch organization details
        NewOrg orgDetails = newOrgService.getOrganizationById(Long.valueOf(orgId));

        if (orgDetails == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(orgDetails);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/orders")
    public ResponseEntity<?> getOrgOrders() {
        // Get current authenticated organization user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OrganizationUserDetails userDetails = (OrganizationUserDetails) authentication.getPrincipal();

        // Get organization ID
        Integer orgId = userDetails.getOrgId();

        // Fetch organization orders - properly handle as a List
        List<Order> orders = orgOrderService.getOrdersByOrgId(Math.toIntExact(Long.valueOf(orgId)));

        if (orders == null || orders.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList()); // Return empty list instead of 404
        }

        // Return the list of orders
        return ResponseEntity.ok(orders);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/dashboard/orders")
    public ResponseEntity<?> getOrgDashboardOrders() {
        // Simply delegate to the existing method
        return getOrgOrders();
    }
}