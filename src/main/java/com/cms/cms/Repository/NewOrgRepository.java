package com.cms.cms.Repository;



import com.cms.cms.model.NewOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;


@Repository
public interface NewOrgRepository extends JpaRepository<NewOrg, Long> {
    Optional<NewOrg> findByWebsiteUsername(String username);

}
