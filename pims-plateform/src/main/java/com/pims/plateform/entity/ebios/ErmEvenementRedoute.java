// ErmEvenementRedoute.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "erm_evenement_redoute")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmEvenementRedoute {

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

    @Column(nullable = false) private String libelle;
    @Builder.Default private Short gravite = 2;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default private String impacts = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "besoins_securite", columnDefinition = "jsonb")
    @Builder.Default private String besoinsSecurite = "[]";
}