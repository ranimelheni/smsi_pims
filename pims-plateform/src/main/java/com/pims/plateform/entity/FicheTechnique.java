package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "fiches_techniques")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FicheTechnique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acteur_id", nullable = false)
    private User acteur;

    @Column(nullable = false, length = 255)
    @Builder.Default private String intitule = "Fiche technique";

    @Column(columnDefinition = "TEXT")
    private String perimetre;

    // Actifs matériels
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_serveurs", columnDefinition = "jsonb")
    @Builder.Default private String actifsServeurs = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_postes", columnDefinition = "jsonb")
    @Builder.Default private String actifsPostes = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_reseau", columnDefinition = "jsonb")
    @Builder.Default private String actifsReseau = "[]";

    // Actifs logiciels
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_applications", columnDefinition = "jsonb")
    @Builder.Default private String actifsApplications = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_licences", columnDefinition = "jsonb")
    @Builder.Default private String actifsLicences = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_bdd", columnDefinition = "jsonb")
    @Builder.Default private String actifsBdd = "[]";

    // Actifs données
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_sauvegardes", columnDefinition = "jsonb")
    @Builder.Default private String actifsSauvegardes = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_stockages", columnDefinition = "jsonb")
    @Builder.Default private String actifsStockages = "[]";

    // Actifs services
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_cloud", columnDefinition = "jsonb")
    @Builder.Default private String actifsCloud = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_acces", columnDefinition = "jsonb")
    @Builder.Default private String actifsAcces = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actifs_certificats", columnDefinition = "jsonb")
    @Builder.Default private String actifsCertificats = "[]";

    // Mesures de sécurité
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "mesures_securite", columnDefinition = "jsonb")
    @Builder.Default private String mesuresSecurite = "{}";

    // Workflow
    @Column(length = 30)
    @Builder.Default private String statut = "brouillon";

    @Column(name = "commentaire_rejet", columnDefinition = "TEXT")
    private String commentaireRejet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_by")
    private User validePar;

    @Column(name = "valide_at") private LocalDateTime valideAt;
    @Column(name = "soumis_at") private LocalDateTime soumisAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}