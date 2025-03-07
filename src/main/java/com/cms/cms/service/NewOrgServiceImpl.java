package com.cms.cms.service;


import com.cms.cms.Repository.NewOrgRepository;
import com.cms.cms.Repository.OrganizationRepository;
import com.cms.cms.model.NewOrg;
import com.cms.cms.model.OrganizationCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NewOrgServiceImpl implements NewOrgService {

    @Autowired
    private NewOrgRepository newOrgRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Use Spring's injected PasswordEncoder

    @Override
    public NewOrg createNewOrg(NewOrg newOrg) {
        // Hash the password before saving
        String hashedPassword = passwordEncoder.encode(newOrg.getWebsitePassword());
        newOrg.setWebsitePassword(hashedPassword);

        // Save organization with hashed password
        return newOrgRepository.save(newOrg);
    }

    @Override
    public List<NewOrg> getAllOrganizations() {
        return newOrgRepository.findAll();
    }

    @Override
    public NewOrg getOrganizationById(Long id) {
        Optional<NewOrg> orgOptional = newOrgRepository.findById(id);
        return orgOptional.orElse(null);
    }
}