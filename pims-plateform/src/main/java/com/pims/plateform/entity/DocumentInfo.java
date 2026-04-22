package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_info")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentInfo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiche_id")
    private FicheProcessus fiche;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    private String reference;

    @Column(nullable = false)
    private String titre;

    @Column(name = "type_document", length = 50)
    @Builder.Default
    private String typeDocument = "procedure";

    @Column(length = 20)
    @Builder.Default
    private String version = "v1.0";

    @Column(length = 30)
    @Builder.Default
    private String statut = "brouillon";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "chemin_fichier")
    private String cheminFichier;

    @Column(name = "nom_fichier")
    private String nomFichier;

    private Long taille;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Column(name = "date_revision")
    private LocalDate dateRevision;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approuve_par")
    private User approuvePar;

    @Column(name = "approuve_at")
    private LocalDateTime approuveAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
     @Column(name = "type_mime")
    private String typeMime;
    @Column(name = "extension")
    private String extension;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}