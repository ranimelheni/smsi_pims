// ErmScenarioStrategique.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "erm_scenario_strategique")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmScenarioStrategique {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private ErmSourceRisque source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objectif_id", nullable = false)
    private ErmObjectifVise objectif;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evenement_id")
    private ErmEvenementRedoute evenement;

    @Column(nullable = false) private String libelle;
    @Column(columnDefinition = "TEXT") private String description;

    @Builder.Default private Short gravite      = 2;
    @Builder.Default private Short vraisemblance = 2;
    @Column(name = "niveau_risque") private Short niveauRisque;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parties_prenantes", columnDefinition = "jsonb")
    @Builder.Default private String partiesPrenantes = "[]";

    @Column(name = "decision_traitement", length = 20)
    @Builder.Default private String decisionTraitement = "traiter";

    @Column(name = "justification_decision", columnDefinition = "TEXT")
    private String justificationDecision;
}