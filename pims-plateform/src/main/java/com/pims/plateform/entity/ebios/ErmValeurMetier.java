// ErmValeurMetier.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "erm_valeur_metier")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmValeurMetier {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entite_id")
    private ErmEntite entite;

    @Column(nullable = false) private String denomination;
    @Column(length = 255)     private String mission;
    @Column(length = 50)      @Builder.Default private String nature = "processus";
    @Column(columnDefinition = "TEXT") private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "besoins_securite", columnDefinition = "jsonb")
    @Builder.Default private String besoinsSecurite =
        "{\"confidentialite\":false,\"integrite\":false,\"disponibilite\":false}";
}