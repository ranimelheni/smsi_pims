// ErmObjectifVise.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_objectif_vise")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmObjectifVise {

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

    @Column(nullable = false)          private String  libelle;
    @Column(columnDefinition = "TEXT") private String  description;

    @Builder.Default private Short   motivation         = 2;
    @Builder.Default private Short   ressource          = 2;
    @Builder.Default private Short   activite           = 2;
    @Column(name = "pertinence_proposee")
    @Builder.Default private Short   pertinenceProposee = 2;
    @Column(name = "pertinence_retenue")
    @Builder.Default private Short   pertinenceRetenue  = 2;
    @Builder.Default private Boolean retenu             = true;
    @Column(name = "justification_rejet", columnDefinition = "TEXT")
    private String justificationRejet;
}