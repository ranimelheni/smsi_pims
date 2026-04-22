package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "communication_publication")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunicationPublication {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publie_par", nullable = false)
    private User publiePar;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @Column(length = 30)
    @Builder.Default
    private String type = "information";

    @Column(length = 20)
    @Builder.Default
    private String priorite = "normale";

    @Column(length = 30)
    @Builder.Default
    private String cible = "tous";

    @Column(name = "publie_at")
    private LocalDateTime publieAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "est_publie")
    @Builder.Default
    private Boolean estPublie = false;

    @Column(name = "est_epingle")
    @Builder.Default
    private Boolean estEpingle = false;

    @Column(name = "a_fichier")
    @Builder.Default
    private Boolean aFichier = false;

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<CommunicationLecture> lectures = new java.util.ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}