package com.cms.cms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@ToString(exclude = "products") // Exclude products from toString to prevent recursion
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

    // Add JsonIgnoreProperties to break circular reference for serialization
    @ManyToMany
    @JoinTable(
            name = "organization_products",
            joinColumns = @JoinColumn(name = "organization_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @JsonIgnoreProperties("organizations")
    private Set<Product> products = new HashSet<>();

    // Custom equals that doesn't use the products collection
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewOrg newOrg = (NewOrg) o;
        return id != null && Objects.equals(id, newOrg.id);
    }

    // Custom hashCode that doesn't use the products collection
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Helper methods for managing the relationship
    public void addProduct(Product product) {
        if (this.products == null) {
            this.products = new HashSet<>();
        }
        this.products.add(product);

        if (product.getOrganizations() == null) {
            product.setOrganizations(new HashSet<>());
        }
        product.getOrganizations().add(this);
    }

    public void removeProduct(Product product) {
        if (this.products != null) {
            this.products.remove(product);
        }

        if (product.getOrganizations() != null) {
            product.getOrganizations().remove(this);
        }
    }
}