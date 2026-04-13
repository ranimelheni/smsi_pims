// ErmSocleSecurite.java
package com.pims.plateform.entity.ebios;

import com.pims.plateform.entity.Organism;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "erm_socle_securite")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErmSocleSecurite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyse_id", nullable = false)
    private ErmAnalyse analyse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(name = "nom_referentiel", nullable = false, length = 100)
    private String nomReferentiel;

    @Column(name = "type_referentiel", length = 100)
    private String typeReferentiel;

    @Column(name = "etat_application", length = 50)
    @Builder.Default private String etatApplication = "partiel";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default private String ecarts = "[]";
}