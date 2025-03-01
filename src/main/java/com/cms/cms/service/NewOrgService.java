package com.cms.cms.service;



import com.cms.cms.Repository.NewOrgRepository;
import com.cms.cms.model.NewOrg;
import com.cms.cms.model.OrganizationCredentials;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.Optional;



import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;



@Service
@Validated
public class NewOrgService {

    @Autowired
    private NewOrgRepository newOrgRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Inject PasswordEncoder

    public NewOrg createNewOrg(@Valid NewOrg newOrg) {
        // Hash the password before saving
        String encodedPassword = passwordEncoder.encode(newOrg.getWebsitePassword());
        newOrg.setWebsitePassword(encodedPassword);
        return newOrgRepository.save(newOrg);
    }



}
