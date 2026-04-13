package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "soa_controle_implementation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SoaControleImplementation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soa_controle_id", nullable = false, unique = true)
    private SoaControle soaControle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(name = "statut_detail", length = 30)
    @Builder.Default private String statutDetail = "non_commence";

    @Column(name = "niveau_maturite")
    @Builder.Default private Short niveauMaturite = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default private String outils = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "regles_gestion", columnDefinition = "jsonb")
    @Builder.Default private String reglesGestion = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default private String procedures = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default private String configurations = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default private String preuves = "[]";

    @Column(columnDefinition = "TEXT") private String notes;
    @Column(length = 255)              private String responsable;
    @Column(name = "date_revue")       private LocalDate dateRevue;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}