package com.cms.cms.Repository;



import com.cms.cms.model.NewOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewOrgRepository extends JpaRepository<NewOrg, Long> {
}
