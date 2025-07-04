package com.cms.cms.service;

import com.cms.cms.model.NewOrg;
import java.util.List;

public interface NewOrgService {
    // Create a new organization
    NewOrg createNewOrg(NewOrg newOrg);

    // Get all organizations
    List<NewOrg> getAllOrganizations();

    // Get an organization by ID
    NewOrg getOrganizationById(Long id);

    // Update an organization
    NewOrg updateOrganization(Long id, NewOrg orgDetails);

    // Find organization by website username
    NewOrg findByWebsiteUsername(String username);

    // Delete an organization
    void deleteOrganization(Long id);

    // Clear organization cache
    void clearCache();
}