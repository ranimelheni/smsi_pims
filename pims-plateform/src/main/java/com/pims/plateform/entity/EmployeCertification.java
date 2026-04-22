package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employe_certification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeCertification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profil_id", nullable = false)
    private EmployeProfil profil;

    @Column(nullable = false)
    private String nom;

    private String organisme;

    @Column(name = "date_obtention")
    private LocalDate dateObtention;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    @Column(name = "chemin_fichier")
    private String cheminFichier;

    @Column(name = "nom_fichier")
    private String nomFichier;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
     @Column(name = "type_mime")
    private String typeMime;
    @Column(name = "extension")
    private String extension;

    @PrePersist protected void onCreate() { uploadedAt = LocalDateTime.now(); }
}