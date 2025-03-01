package com.cms.cms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org")
public class OrganizationController {

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/dashboard")
    public ResponseEntity<?> getOrgDashboard() {
        // Organization-specific logic
        return ResponseEntity.ok("Organization Dashboard Data");
    }
}
