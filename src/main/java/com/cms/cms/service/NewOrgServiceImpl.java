package com.cms.cms.service;


import com.cms.cms.Repository.NewOrgRepository;
import com.cms.cms.model.NewOrg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class NewOrgServiceImpl implements NewOrgService {

    @Autowired
    private NewOrgRepository newOrgRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public NewOrg createNewOrg(NewOrg newOrg) {
        String hashedPassword = passwordEncoder.encode(newOrg.getWebsitePassword());
        newOrg.setWebsitePassword(hashedPassword);

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