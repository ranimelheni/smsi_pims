package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "actifs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Actif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(nullable = false, length = 255)
    private String nom;

    @Column(length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 30)
    private String categorie;

    @Column(nullable = false)
    @Builder.Default private Integer confidentialite = 2;

    @Column(nullable = false)
    @Builder.Default private Integer integrite = 2;

    @Column(nullable = false)
    @Builder.Default private Integer disponibilite = 2;

    @Column(length = 255) private String proprietaire;
    @Column(length = 255) private String gestionnaire;
    @Column(length = 255) private String localisation;

    @Column(length = 30)
    @Builder.Default private String source = "manuel";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiche_processus_id")
    private FicheProcessus ficheProcessus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiche_technique_id")
    private FicheTechnique ficheTechnique;

    @Column(name = "section_source", length = 50)
    private String sectionSource;

    @Column(length = 30)
    @Builder.Default private String statut = "en_attente_validation";

    @Column(name = "commentaire_rejet", columnDefinition = "TEXT")
    private String commentaireRejet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_by")
    private User validePar;

    @Column(name = "valide_at")
    private LocalDateTime valideAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public int getNoteGlobale() {
        return Math.max(confidentialite, Math.max(integrite, disponibilite));
    }

    public String getNiveauCritique() {
        return switch (getNoteGlobale()) {
            case 4 -> "Secret";
            case 3 -> "Confidentiel";
            case 2 -> "Interne";
            default -> "Public";
        };
    }
}