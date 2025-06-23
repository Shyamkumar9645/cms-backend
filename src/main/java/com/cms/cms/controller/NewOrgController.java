package com.cms.cms.controller;

import com.cms.cms.dto.OrganizationDTO;
import com.cms.cms.model.NewOrg;
import com.cms.cms.service.NewOrgService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/new-org")
@CrossOrigin(origins = "*") // Enable CORS for all origins - adjust this for security
public class NewOrgController {
    @Autowired
    private NewOrgService newOrgService;
    private static final Logger logger = LoggerFactory.getLogger(NewOrgController.class);


    // Get all organizations - Using DTO pattern
    @GetMapping
    public ResponseEntity<List<OrganizationDTO>> getAllOrganizations() {
        try {
            List<NewOrg> organizations = newOrgService.getAllOrganizations();

            // Convert entities to DTOs
            List<OrganizationDTO> orgDTOs = organizations.stream()
                    .map(OrganizationDTO::fromEntity)
                    .collect(Collectors.toList());

            return new ResponseEntity<>(orgDTOs, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get organization by ID - Using DTO pattern
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDTO> getOrganizationById(@PathVariable Long id) {
        try {
            NewOrg org = newOrgService.getOrganizationById(id);
            if (org != null) {
                // Convert entity to DTO
                OrganizationDTO orgDTO = OrganizationDTO.fromEntity(org);
                return new ResponseEntity<>(orgDTO, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Update organization
    @PutMapping("/{id}")
    public ResponseEntity<NewOrg> updateOrganization(@PathVariable Long id, @Valid @RequestBody NewOrg orgDetails) {
        try {
            NewOrg updatedOrg = newOrgService.updateOrganization(id, orgDetails);
            return new ResponseEntity<>(updatedOrg, HttpStatus.OK);
        } catch (Exception e) {
            logger.info("Error retrieving organizations", e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Create new organization
    @PostMapping("/submit")
    public ResponseEntity<?> submitNewOrg(@Valid @RequestBody NewOrg newOrg) {
        try {
            NewOrg createdOrg = newOrgService.createNewOrg(newOrg);
            return new ResponseEntity<>(createdOrg, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Validation exception handler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }
}