package com.cms.cms.model;

import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;

public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long expiresIn;
    private Collection<? extends GrantedAuthority> roles;
    private String userType;

    public JwtResponse(String token, Long expiresIn, Collection<? extends GrantedAuthority> roles, String userType) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.roles = roles;
        this.userType = userType;
    }

    // Getters and setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Collection<? extends GrantedAuthority> getRoles() {
        return roles;
    }

    public void setRoles(Collection<? extends GrantedAuthority> roles) {
        this.roles = roles;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}