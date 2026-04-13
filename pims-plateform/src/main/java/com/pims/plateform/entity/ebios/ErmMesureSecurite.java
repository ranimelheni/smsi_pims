// ErmMesureSecurite.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity @Table(name = "erm_mesure_securite")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmMesureSecurite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(nullable = false) private String libelle;
    @Column(columnDefinition = "TEXT") private String description;

    @Column(name = "type_mesure", length = 50)
    @Builder.Default private String typeMesure = "preventive";

    @Column(name = "frein_difficulte", columnDefinition = "TEXT") private String freinDifficulte;
    @Column(name = "cout_complexite")  @Builder.Default private Short coutComplexite = 2;
    @Column(name = "echeance_mois")    private Short echeanceMois;
    @Column(length = 25)               @Builder.Default private String statut = "planifiee";
    @Column(length = 100)              private String responsable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scenarios_couverts", columnDefinition = "jsonb")
    @Builder.Default private String scenariosCouvert = "[]";

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}