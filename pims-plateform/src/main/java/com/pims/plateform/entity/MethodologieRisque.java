package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "methodologie_risque")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MethodologieRisque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false, unique = true)
    private Organism organism;

    @Column(nullable = false, length = 30)
    @Builder.Default private String methode = "ebios_rm";

    @Column(name = "methode_custom", length = 255)
    private String methodeCustom;

    @Column(name = "echelle_probabilite", nullable = false)
    @Builder.Default private Integer echelleProbabilite = 4;

    @Column(name = "echelle_impact", nullable = false)
    @Builder.Default private Integer echelleImpact = 4;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels_probabilite", columnDefinition = "jsonb")
    @Builder.Default private String labelsProbabilite =
        "[\"Rare\",\"Peu probable\",\"Probable\",\"Quasi-certain\"]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels_impact", columnDefinition = "jsonb")
    @Builder.Default private String labelsImpact =
        "[\"Négligeable\",\"Limité\",\"Important\",\"Critique\"]";

    @Column(name = "seuil_acceptable", nullable = false)
    @Builder.Default private Integer seuilAcceptable = 6;

    @Column(name = "seuil_eleve", nullable = false)
    @Builder.Default private Integer seuilEleve = 12;

    @Column(name = "formule_calcul", length = 20)
    @Builder.Default private String formuleCalcul = "probabilite_x_impact";

    @Column(columnDefinition = "TEXT") private String justification;
    @Column(name = "perimetre_risque",     columnDefinition = "TEXT") private String perimetreRisque;
    @Column(name = "objectifs_risque",     columnDefinition = "TEXT") private String objectifsRisque;
    @Column(name = "criteres_acceptation", columnDefinition = "TEXT") private String criteresAcceptation;

    @Column(length = 20)
    @Builder.Default private String statut = "propose";

    @Column(name = "commentaire_direction", columnDefinition = "TEXT")
    private String commentaireDirection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_by")
    private User validePar;

    @Column(name = "valide_at") private LocalDateTime valideAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propose_by")
    private User proposePar;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}