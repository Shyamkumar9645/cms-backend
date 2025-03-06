package com.cms.cms.controller;


import com.cms.cms.model.NewOrg;
import com.cms.cms.service.NewOrgService;
import com.cms.cms.service.OrganizationUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/org/dashboard")
public class OrgDashboardController {

    @Autowired
    private NewOrgService newOrgService;

    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/profile")
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

        // Fetch organization orders (you'll need to create this service)
        // List<Order> orders = orderService.getOrdersByOrgId(orgId);

        // For now, return a placeholder
        return ResponseEntity.ok("Orders for organization ID: " + orgId);
    }
}