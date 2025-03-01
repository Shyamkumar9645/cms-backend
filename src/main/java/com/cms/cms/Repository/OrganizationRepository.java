package com.cms.cms.Repository;


import com.cms.cms.model.OrganizationCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<OrganizationCredentials, Long> {
    Optional<OrganizationCredentials> findByUsername(String username);
}
