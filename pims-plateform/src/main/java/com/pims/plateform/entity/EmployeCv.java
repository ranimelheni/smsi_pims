package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employe_cv")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeCv {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profil_id", nullable = false)
    private EmployeProfil profil;

    @Column(name = "nom_fichier", nullable = false)
    private String nomFichier;

    @Column(name = "chemin_fichier", nullable = false)
    private String cheminFichier;

    private Long taille;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    @Column(name = "type_mime")
    private String typeMime;
    @Column(name = "extension")
    private String extension;

    @PrePersist protected void onCreate() { uploadedAt = LocalDateTime.now(); }
}