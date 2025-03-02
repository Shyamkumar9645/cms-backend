package com.cms.cms.controller;

import com.cms.cms.model.NewOrg;
import com.cms.cms.service.NewOrgService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/new-org")
@CrossOrigin(origins = "*") // Enable CORS for all origins - adjust this for security
public class NewOrgController {

    @Autowired
    private NewOrgService newOrgService;

    // Get all organizations
    @GetMapping
    public ResponseEntity<List<NewOrg>> getAllOrganizations() {
        try {
            List<NewOrg> organizations = newOrgService.getAllOrganizations();
            return new ResponseEntity<>(organizations, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Get organization by ID
    @GetMapping("/{id}")
    public ResponseEntity<NewOrg> getOrganizationById(@PathVariable Long id) {
        try {
            NewOrg org = newOrgService.getOrganizationById(id);
            if (org != null) {
                return new ResponseEntity<>(org, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Create new organization (your existing endpoint)
    @PostMapping("/submit")
    public ResponseEntity<?> submitNewOrg(@Valid @RequestBody NewOrg newOrg) {
        try {
            NewOrg createdOrg = newOrgService.createNewOrg(newOrg);
            return new ResponseEntity<>(createdOrg, HttpStatus.CREATED);  // Return 201 Created
        } catch (Exception e) { // Catch any other exceptions
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR); // 500
        }
    }

    // Validation exception handler (your existing handler)
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