package com.cms.cms.service;


import com.cms.cms.Repository.NewOrgRepository;
import com.cms.cms.model.NewOrg;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    public NewOrg updateOrganization(Long id, NewOrg orgDetails) {
        Optional<NewOrg> orgData = newOrgRepository.findById(id);

        if (orgData.isPresent()) {
            NewOrg existingOrg = orgData.get();

            // Update fields from orgDetails
            existingOrg.setOrganizationName(orgDetails.getOrganizationName());
            existingOrg.setConstitution(orgDetails.getConstitution());
            existingOrg.setAddressLine1(orgDetails.getAddressLine1());
            existingOrg.setCity(orgDetails.getCity());
            existingOrg.setZip(orgDetails.getZip());
            existingOrg.setGstNumber(orgDetails.getGstNumber());
            existingOrg.setPanNumber(orgDetails.getPanNumber());
            existingOrg.setDrugLicense1(orgDetails.getDrugLicense1());
            existingOrg.setDrugLicense2(orgDetails.getDrugLicense2());
            existingOrg.setStatus(orgDetails.getStatus());

            // Update representative details
            existingOrg.setRepresentativeFirstName(orgDetails.getRepresentativeFirstName());
            existingOrg.setRepresentativeLastName(orgDetails.getRepresentativeLastName());
            existingOrg.setRepresentativeEmail(orgDetails.getRepresentativeEmail());
            existingOrg.setRepresentativeNumber(orgDetails.getRepresentativeNumber());
            existingOrg.setRepresentativeAadhar(orgDetails.getRepresentativeAadhar());

            // Note: We're not updating the password here for security reasons
            // If password update is needed, implement a separate method

            return newOrgRepository.save(existingOrg);
        } else {
            throw new RuntimeException("Organization not found with id " + id);
        }
    }
}