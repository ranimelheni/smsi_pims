// ErmSourceRisque.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_source_risque")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmSourceRisque {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(nullable = false)        private String libelle;
    @Column(length = 50)
    @Builder.Default private String  categorie   = "externe";
    @Column(columnDefinition = "TEXT") private String description;
    @Builder.Default private Boolean retenu      = true;
}