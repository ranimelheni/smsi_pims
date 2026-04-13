// ErmPartiePrenante.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_partie_prenante_ecosysteme")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmPartiePrenante{

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(nullable = false, length = 100) private String libelle;
    @Column(length = 50)
    @Builder.Default private String categorie = "fournisseur";
    @Column(columnDefinition = "TEXT") private String description;

    @Builder.Default private Short dependance   = 2;
    @Builder.Default private Short penetration  = 2;
    @Column(name = "maturite_cyber")
    @Builder.Default private Short maturiteCyber = 2;
    @Builder.Default private Short confiance     = 2;
    @Column(name = "fiabilite_cyber") private Float fiabiliteCyber;
    @Builder.Default private Boolean retenu = true;
}