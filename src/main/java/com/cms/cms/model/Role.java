package com.cms.cms.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "roles")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String name;

    // This method is actually redundant since @Data already provides getters and setters
    // But if you want to keep it explicitly:
    public String getName() {
        return name;
    }

    // This method had a bug - no parameter and didn't assign anything
    public void setName(String name) {
        this.name = name;
    }
}