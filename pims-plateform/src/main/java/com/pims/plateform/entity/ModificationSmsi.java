package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "modifications_smsi")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ModificationSmsi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(nullable = false, length = 255) private String titre;
    @Column(columnDefinition = "TEXT")      private String description;
    @Column(name = "type_modification", length = 50) private String typeModification;
    @Column(columnDefinition = "TEXT")      private String impacts;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default private String actions = "[]";

    @Column(length = 30) @Builder.Default private String statut = "en_analyse";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "declare_by")
    private User declareBy;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}