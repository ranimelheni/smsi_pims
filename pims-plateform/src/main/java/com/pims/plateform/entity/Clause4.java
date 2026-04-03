package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "clause4")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Clause4 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false, unique = true)
    private Organism organism;

    // 4.1 Enjeux
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enjeux_externes", columnDefinition = "jsonb")
    @Builder.Default private String enjeuxExternes = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enjeux_internes", columnDefinition = "jsonb")
    @Builder.Default private String enjeuxInternes = "[]";

    // 4.2 Parties intéressées
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parties_interessees", columnDefinition = "jsonb")
    @Builder.Default private String partiesInteressees = "[]";

    // 4.3 Domaine d'application
    @Column(name = "perimetre_smsi",            columnDefinition = "TEXT") private String perimetreSmsi;
    @Column(name = "perimetre_pims",            columnDefinition = "TEXT") private String perimetrePims;
    @Column(name = "sites_concernes",           columnDefinition = "TEXT") private String sitesConcernes;
    @Column(name = "activites_exclues",         columnDefinition = "TEXT") private String activitesExclues;
    @Column(name = "justification_exclusions",  columnDefinition = "TEXT") private String justificationExclusions;
    @Column(name = "interfaces_dependances",    columnDefinition = "TEXT") private String interfacesDependances;
    @Column(name = "responsable_traitement",    length = 30)
    @Builder.Default private String responsableTraitement = "responsable";

    // 4.4 Ressources et politiques
    @Column(name = "engagement_direction",      columnDefinition = "TEXT") private String engagementDirection;
    @Column(name = "politique_securite",        columnDefinition = "TEXT") private String politiqueSecurite;
    @Column(name = "politique_confidentialite", columnDefinition = "TEXT") private String politiqueConfidentialite;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ressources_humaines",   columnDefinition = "jsonb")
    @Builder.Default private String ressourcesHumaines = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ressources_logicielles", columnDefinition = "jsonb")
    @Builder.Default private String ressourcesLogicielles = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ressources_materielles", columnDefinition = "jsonb")
    @Builder.Default private String ressourcesMaterielles = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "procedures", columnDefinition = "jsonb")
    @Builder.Default private String procedures = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "outils_protection", columnDefinition = "jsonb")
    @Builder.Default private String outilsProtection = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "objectifs_smsi", columnDefinition = "jsonb")
    @Builder.Default private String objectifsSmsi = "[]";

    @Column(name = "date_revue") private LocalDate dateRevue;
    @Column(name = "version", length = 10) @Builder.Default private String version = "v1.0";
    @Column(name = "statut",  length = 20) @Builder.Default private String statut  = "brouillon";

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}