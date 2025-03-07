package com.cms.cms.service;

import com.cms.cms.model.NewOrg;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class OrganizationUserDetails implements UserDetails {
    private Long id;
    private Integer orgId;
    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    public OrganizationUserDetails(Long id, String username, String password,
                                   Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.orgId = id.intValue(); // Use the same ID as both orgId and id
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public static OrganizationUserDetails build(NewOrg org) {
        // Assign ROLE_ORGANIZATION authority
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_ORGANIZATION")
        );

        return new OrganizationUserDetails(
                org.getId(),
                org.getWebsiteUsername(),
                org.getWebsitePassword(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() {
        return id;
    }

    public Integer getOrgId() {
        return orgId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}