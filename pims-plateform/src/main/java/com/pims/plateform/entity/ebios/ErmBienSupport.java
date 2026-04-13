// ErmBienSupport.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "erm_bien_support")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmBienSupport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valeur_metier_id", nullable = false)
    private ErmValeurMetier valeurMetier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entite_id")
    private ErmEntite entite;

    @Column(nullable = false)  private String denomination;
    @Column(name = "type_bien", length = 50)
    @Builder.Default private String typeBien = "materiel";
    @Column(columnDefinition = "TEXT") private String description;
    @Column(length = 100) private String responsable;
}