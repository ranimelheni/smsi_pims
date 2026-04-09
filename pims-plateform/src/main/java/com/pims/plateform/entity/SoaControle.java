package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "soa_controles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SoaControle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soa_id", nullable = false)
    private Soa soa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(nullable = false, length = 10)  private String annexe;
    @Column(name = "annexe_label", nullable = false, length = 100) private String annexeLabel;
    @Column(name = "controle_id",  nullable = false, length = 15)  private String controleId;
    @Column(name = "controle_label", nullable = false, length = 255) private String controleLabel;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false, length = 20)
    @Builder.Default private String norme = "iso27001";

    @Column(nullable = false)
    @Builder.Default private Boolean inclus = true;

    @Column(name = "justification_exclusion", columnDefinition = "TEXT")
    private String justificationExclusion;

    @Column(name = "reference_doc", length = 255) private String referenceDoc;

    @Column(name = "statut_impl", length = 20)
    @Builder.Default private String statutImpl = "planifie";

    @Column(length = 255) private String responsable;
    @Column private LocalDate echeance;
}