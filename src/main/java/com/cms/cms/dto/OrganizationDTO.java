package com.cms.cms.dto;

import com.cms.cms.model.NewOrg;
import com.cms.cms.model.Product;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class OrganizationDTO {
    private Long id;
    private String organizationName;
    private String constitution;
    private String addressLine1;
    private String city;
    private String zip;
    private String gstNumber;
    private String panNumber;
    private String drugLicense1;
    private String drugLicense2;
    private String representativeFirstName;
    private String representativeLastName;
    private String representativeEmail;
    private String representativeNumber;
    private String representativeAadhar;
    private String websiteUsername;
    private LocalDateTime createdAt;
    private String status;
    private List<ProductSummaryDTO> products = new ArrayList<>();

    // Static converter method
    public static OrganizationDTO fromEntity(NewOrg org) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setId(org.getId());
        dto.setOrganizationName(org.getOrganizationName());
        dto.setConstitution(org.getConstitution());
        dto.setAddressLine1(org.getAddressLine1());
        dto.setCity(org.getCity());
        dto.setZip(org.getZip());
        dto.setGstNumber(org.getGstNumber());
        dto.setPanNumber(org.getPanNumber());
        dto.setDrugLicense1(org.getDrugLicense1());
        dto.setDrugLicense2(org.getDrugLicense2());
        dto.setRepresentativeFirstName(org.getRepresentativeFirstName());
        dto.setRepresentativeLastName(org.getRepresentativeLastName());
        dto.setRepresentativeEmail(org.getRepresentativeEmail());
        dto.setRepresentativeNumber(org.getRepresentativeNumber());
        dto.setRepresentativeAadhar(org.getRepresentativeAadhar());
        dto.setWebsiteUsername(org.getWebsiteUsername());
        dto.setCreatedAt(org.getCreatedAt());
        dto.setStatus(org.getStatus());

        // Convert products to DTOs if they exist
        if (org.getProducts() != null) {
            dto.setProducts(org.getProducts().stream()
                    .map(ProductSummaryDTO::fromEntity)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    // Nested DTO for a simplified product view
    @Data
    public static class ProductSummaryDTO {
        private Long id;
        private String name;
        private String type;
        private String description;

        public static ProductSummaryDTO fromEntity(Product product) {
            ProductSummaryDTO dto = new ProductSummaryDTO();
            dto.setId(product.getId());
            dto.setName(product.getName());
            dto.setType(product.getType());
            dto.setDescription(product.getDescription());
            return dto;
        }
    }
}