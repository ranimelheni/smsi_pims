package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employe_profil")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeProfil {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    private String poste;
    private String departement;

    @Column(name = "date_entree")
    private LocalDate dateEntree;

    private String telephone;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "statut_evaluation", length = 30)
    @Builder.Default
    private String statutEvaluation = "non_evalue";

    @Column(name = "score_global", precision = 5, scale = 2)
    private BigDecimal scoreGlobal;

    @Column(name = "commentaire_admin", columnDefinition = "TEXT")
    private String commentaireAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evalue_par")
    private User evaluePar;

    @Column(name = "evalue_at")
    private LocalDateTime evalueAt;

    @OneToMany(mappedBy = "profil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<EmployeCv> cvs = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "profil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<EmployeCertification> certifications = new java.util.ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}