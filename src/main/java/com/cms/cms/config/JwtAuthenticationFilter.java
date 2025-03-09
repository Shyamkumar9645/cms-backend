package com.cms.cms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider tokenProvider;

    @Autowired
    @Qualifier("userDetailsServiceImpl")
    private UserDetailsService adminUserDetailsService;

    @Autowired
    @Qualifier("organizationUserDetailsService")
    private UserDetailsService orgUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   @Qualifier("userDetailsServiceImpl") UserDetailsService adminUserDetailsService,
                                   @Qualifier("organizationUserDetailsService") UserDetailsService orgUserDetailsService) {
        this.tokenProvider = tokenProvider;
        this.adminUserDetailsService = adminUserDetailsService;
        this.orgUserDetailsService = orgUserDetailsService;
    }
    // In JwtAuthenticationFilter.java
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            logger.info("Request path: {}", request.getRequestURI());
            logger.info("JWT present: {}", jwt != null);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                // Get username and user type from the token
                String username = tokenProvider.getUsernameFromToken(jwt);
                String userType = tokenProvider.getUserTypeFromToken(jwt);

                logger.info("JWT validated for user: {}, type: {}", username, userType);

                // Select the appropriate UserDetailsService based on the user type
                UserDetailsService userDetailsService = "ORGANIZATION".equals(userType)
                        ? orgUserDetailsService
                        : adminUserDetailsService;

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                logger.info("User loaded, authorities: {}", userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.info("Authentication set in security context");
            } else if (StringUtils.hasText(jwt)) {
                logger.warn("JWT validation failed");
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}