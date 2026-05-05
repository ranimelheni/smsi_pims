package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_session")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditSession {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auditeur_id", nullable = false)
    private User auditeur;

    @Column(nullable = false)
    @Builder.Default
    private String titre = "Analyse des écarts";

    @Column(length = 20)
    @Builder.Default
    private String version = "v1.0";

    @Column(length = 20)
    @Builder.Default
    private String norme = "iso27001";

    @Column(length = 30)
    @Builder.Default
    private String statut = "en_cours";

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "commentaire_global", columnDefinition = "TEXT")
    private String commentaireGlobal;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}