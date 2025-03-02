package com.cms.cms.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "organizations")
@Data
public class NewOrg {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Organization name is required")
    @Column(name = "org_name")
    private String organizationName;

    @NotBlank(message = "Constitution is required")
    @Column(name = "constitution")
    private String constitution;

    @NotBlank(message = "Address Line 1 is required")
    @Column(name = "address1")
    private String addressLine1;

    @NotBlank(message = "City is required")
    @Column(name = "city")
    private String city;

    @NotBlank(message = "Zip code is required")
    @Column(name = "zip")
    private String zip;

    @Column(name = "gst")
    private String gstNumber;

    @Column(name = "pan")
    private String panNumber;

    @Column(name = "drug1")
    private String drugLicense1;

    @Column(name = "drug2")
    private String drugLicense2;

    @NotBlank(message = "Representative First Name is required")
    @Column(name = "rep_fname")
    private String representativeFirstName;

    @NotBlank(message = "Representative Last Name is required")
    @Column(name = "rep_lname")
    private String representativeLastName;

    @NotBlank(message = "Representative Email is required")
    @Email(message = "Invalid email format")
    @Column(name = "rep_email")
    private String representativeEmail;

    @NotBlank(message = "Representative Aadhar is required")
    @Size(min = 12, max = 12, message = "Aadhar number must be 12 digits")
    @Column(name = "rep_aadhar")
    private String representativeAadhar;

    @NotBlank(message = "Representative Number is required")
    @Size(min = 10, max = 10, message = "Phone number must be 10 digits")
    @Column(name = "rep_number")
    private String representativeNumber;

    @NotBlank(message = "Website Username is required")
    @Column(name = "web_uname")
    private String websiteUsername;

    @NotBlank(message = "Website Password is required")
    @Column(name = "web_password")
    private String websitePassword;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "status")
    private String status;
}