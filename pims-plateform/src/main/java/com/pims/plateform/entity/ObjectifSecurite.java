package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "objectifs_securite")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObjectifSecurite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(nullable = false, length = 255) private String titre;
    @Column(columnDefinition = "TEXT")      private String description;
    @Column(name = "lien_politique", columnDefinition = "TEXT") private String lienPolitique;
    @Column(length = 255) private String responsable;
    @Column(columnDefinition = "TEXT") private String ressources;
    @Column private LocalDate echeance;
    @Column(name = "moyen_evaluation", columnDefinition = "TEXT") private String moyenEvaluation;
    @Column(length = 30) @Builder.Default private String statut = "planifie";
    @Column @Builder.Default private Integer avancement = 0;
    @Column(columnDefinition = "TEXT") private String commentaire;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}