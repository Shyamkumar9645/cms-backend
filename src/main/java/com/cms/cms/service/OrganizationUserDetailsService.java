package com.cms.cms.service;


import com.cms.cms.Repository.OrganizationRepository;
import com.cms.cms.model.OrganizationCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("organizationUserDetailsService")  // Give it a distinct name
public class OrganizationUserDetailsService implements UserDetailsService {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        OrganizationCredentials org = organizationRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Organization not found with username: " + username));

        return OrganizationUserDetails.build(org);
    }
}