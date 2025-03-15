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

    NewOrg updateOrganization(Long id, NewOrg orgDetails);
}