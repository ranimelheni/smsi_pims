// ErmRisqueResiduel.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_risque_residuel")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmRisqueResiduel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_strategique_id", nullable = false)
    private ErmScenarioStrategique scenarioStrategique;

    @Column(name = "gravite_residuelle",       nullable = false) private Short graviteResiduelle;
    @Column(name = "vraisemblance_residuelle",  nullable = false) private Short vraisemblanceResiduelle;
    @Column(name = "niveau_risque_residuel",    nullable = false) private Short niveauRisqueResiduel;

    @Column(length = 20) @Builder.Default private String decision = "traiter";
    @Column(columnDefinition = "TEXT")         private String justification;
}