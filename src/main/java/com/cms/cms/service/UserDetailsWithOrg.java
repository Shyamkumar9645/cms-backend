package com.cms.cms.service;


import org.springframework.security.core.userdetails.UserDetails;

/**
 * Interface for user details with organization ID
 * This decouples the class from specific implementation
 */
public interface UserDetailsWithOrg extends UserDetails {
    Long getOrgId();
}