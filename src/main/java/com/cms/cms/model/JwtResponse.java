package com.cms.cms.model;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Data
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long expiresIn;
    private Collection<? extends GrantedAuthority> roles;

    public JwtResponse(String token, Long expiresIn, Collection<? extends GrantedAuthority> roles) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.roles = roles;
    }
}
