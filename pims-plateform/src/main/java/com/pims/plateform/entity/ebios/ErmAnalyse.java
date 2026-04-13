// ErmAnalyse.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import com.pims.plateform.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "erm_analyse")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmAnalyse {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    @Builder.Default private String titre   = "Analyse EBIOS RM";
    @Column(length = 10)
    @Builder.Default private String version = "v1.0";
    @Column(length = 20)
    @Builder.Default private String statut  = "en_cours";

    @Column(name = "date_debut") private LocalDate dateDebut;
    @Column(name = "date_fin")   private LocalDate dateFin;

    @Column(name = "seuil_acceptable")    @Builder.Default private Integer seuilAcceptable    = 6;
    @Column(name = "seuil_eleve")         @Builder.Default private Integer seuilEleve          = 12;
    @Column(name = "echelle_probabilite") @Builder.Default private Integer echelleProbabilite  = 4;
    @Column(name = "echelle_impact")      @Builder.Default private Integer echelleImpact       = 4;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels_probabilite", columnDefinition = "jsonb")
    @Builder.Default private String labelsProbabilite =
        "[\"Rare\",\"Peu probable\",\"Probable\",\"Quasi-certain\"]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels_impact", columnDefinition = "jsonb")
    @Builder.Default private String labelsImpact =
        "[\"Négligeable\",\"Limité\",\"Important\",\"Critique\"]";

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}