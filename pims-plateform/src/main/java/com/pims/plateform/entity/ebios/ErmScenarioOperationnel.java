// ErmScenarioOperationnel.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_scenario_operationnel")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmScenarioOperationnel {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bien_support_id")
    private ErmBienSupport bienSupport;

    @Column(nullable = false)  private String libelle;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "canal_exfiltration", length = 50) private String canalExfiltration;

    @Builder.Default private Short vraisemblance = 2;
    @Builder.Default private Short gravite       = 2;
    @Column(name = "niveau_risque") private Short niveauRisque;
}