// ErmEntite.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_entite")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmEntite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(name = "nom_entite", nullable = false, length = 100)
    private String nomEntite;

    @Column(name = "type_entite", length = 50)
    @Builder.Default private String typeEntite = "interne";

    @Column(length = 100) private String responsable;
    @Column(columnDefinition = "TEXT") private String description;
}