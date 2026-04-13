// ErmActionElementaire.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_action_elementaire")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmActionElementaire {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_op_id", nullable = false)
    private ErmScenarioOperationnel scenarioOp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(nullable = false) private Short  numero;
    @Column(nullable = false) private String libelle;
    @Column(columnDefinition = "TEXT") private String description;

    @Column(name = "mode_operatoire", length = 50)
    @Builder.Default private String modeOperatoire = "logique";

    @Column(name = "probabilite_succes")
    @Builder.Default private Short probabiliteSucces = 2;
    @Builder.Default private Short difficulte        = 2;

    @Column(name = "vraisemblance_action") private Short vraisemblanceAction;
    @Column(columnDefinition = "TEXT")     private String prerequis;
    public String getPrerequisite() { return this.prerequis; }
public void setPrerequisite(String v) { this.prerequis = v; }
}