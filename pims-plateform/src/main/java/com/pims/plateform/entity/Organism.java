package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "organisms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organism {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(length = 100)
    private String secteur;

    @Column(name = "type_org", length = 100)
    private String typeOrg;

@Enumerated(EnumType.STRING)
@Column(name = "audit_type", length = 20)
private AuditType auditType;

    @Column(length = 20)
    private String siret;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(length = 100)
    @Builder.Default
    private String pays = "France";

    @Column(name = "email_contact", length = 255)
    private String emailContact;

    @Column(length = 20)
    private String telephone;

    @Column(name = "site_web", length = 255)
    private String siteWeb;

    @Column(length = 50)
    private String taille;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "date_audit")
    private LocalDate dateAudit;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "organism", fetch = FetchType.LAZY)
    private List<User> users;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

   
}