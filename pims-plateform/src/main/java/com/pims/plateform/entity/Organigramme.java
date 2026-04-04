package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organigrammes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organigramme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @Column(name = "nom_fichier", nullable = false, length = 255)
    private String nomFichier;

    @Column(name = "type_fichier", nullable = false, length = 20)
    private String typeFichier;

    @Column(nullable = false)
    private byte[] contenu;

    private Long taille;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "is_active")
    @Builder.Default private Boolean isActive = true;

    @PrePersist protected void onCreate() { uploadedAt = LocalDateTime.now(); }
}