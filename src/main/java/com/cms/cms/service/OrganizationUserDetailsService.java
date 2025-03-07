package com.cms.cms.service;


import com.cms.cms.Repository.OrganizationRepository;
import com.cms.cms.model.NewOrg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("organizationUserDetailsService")
public class OrganizationUserDetailsService implements UserDetailsService {

    @Autowired
    private OrganizationRepository organizationRepository; // This repository should query the organizations table

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Find the organization by web_uname
        NewOrg org = organizationRepository.findByWebsiteUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Organization not found with username: " + username));

        // Build UserDetails from organization data
        return OrganizationUserDetails.build(org);
    }
}