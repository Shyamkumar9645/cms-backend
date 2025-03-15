package com.cms.cms.Repository;

import com.cms.cms.model.NewOrg;  // Add this import for NewOrg
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<NewOrg, Long> {
    Optional<NewOrg> findByWebsiteUsername(String username);
    Optional<NewOrg> findByRepresentativeEmail(String email);
}