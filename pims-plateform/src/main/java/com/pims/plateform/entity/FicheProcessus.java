package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "fiches_processus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FicheProcessus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relations ──────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pilote_id")
    private User pilote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_by")
    private User validePar;

    // ── Section 0 – Méta ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
@Column(name = "audit_type", nullable = false)
private AuditType auditType;

    @Column(length = 10)
    @Builder.Default
    private String version = "v1.0";

    @Column(length = 30)
    @Builder.Default
    private String statut = "brouillon";

    // ── Section 1 – Identification ─────────────────────────────────────────
    @Column(nullable = false, length = 255)
    private String intitule;

    @Column(length = 50)
    private String code;

    @Column(name = "type_processus", length = 30)
    private String typeProcessus;

    @Column(length = 100)
    private String domaine;

    @Column(columnDefinition = "TEXT")
    private String activites;

    // ── Section 2 – Finalité & Bénéficiaires ──────────────────────────────
    @Column(columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String finalite = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String beneficiaires = "[]";

    // ── Section 3 – Déclencheurs ───────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String declencheurs = "[]";

    // ── Section 4 – Éléments d'entrée / sortie ─────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "elements_entree", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String elementsEntree = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "elements_sortie_intentionnels", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String elementsSortieIntentionnels = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "elements_sortie_non_intentionnels", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String elementsSortieNonIntentionnels = "[]";

    // ── Section 5 – Informations documentées ──────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "informations_documentees", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String informationsDocumentees = "[]";

    // ── Section 6 – Contraintes ────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contraintes_reglementaires", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String contraintesReglementaires = "[]";

    @Column(name = "contraintes_internes", columnDefinition = "TEXT")
    private String contraintesInternes;

    @Column(name = "contraintes_temporelles", columnDefinition = "TEXT")
    private String contraintesTemporelles;

    @Column(name = "contraintes_techniques", columnDefinition = "TEXT")
    private String contraintegTechniques;

    // ── Section 7 – Acteurs & Ressources ──────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String acteurs = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String ressources = "[]";

    // ── Section 8 – Objectifs / KPI ───────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "objectifs_kpi", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String objectifsKpi = "[]";

    // ── Section 9 – Moyens de surveillance & mesure ────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "moyens_surveillance", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String moyensSurveillance = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "moyens_mesure", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String moyensMesure = "[]";

    // ── Section 10 – Interactions ──────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String interactions = "[]";

    // ── Section 11 – Risques ───────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String risques = "[]";

    @Column(name = "note_max")
    private Integer noteMax;

    @Column(name = "risque_dominant", length = 50)
    private String risqueDominant;

    // ── Section 12 – Opportunités ─────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String opportunites = "[]";

    // ── DPO ───────────────────────────────────────────────────────────────
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data_dpo", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private String dataDpo = "{}";

    // ── Workflow ──────────────────────────────────────────────────────────
    @Column(name = "soumis_at")
    private LocalDateTime soumisAt;

    @Column(name = "valide_at")
    private LocalDateTime valideAt;

    @Column(name = "commentaire_rejet", columnDefinition = "TEXT")
    private String commentaireRejet;

    // ── Audit ─────────────────────────────────────────────────────────────
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    @Column(name = "dpo_at")
private LocalDateTime dpoAt;

public void setDpoAt(LocalDateTime dpoAt) {
    this.dpoAt = dpoAt;
}

public LocalDateTime getDpoAt() {
    return dpoAt;
}
}