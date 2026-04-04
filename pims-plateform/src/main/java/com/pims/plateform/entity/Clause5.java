package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "clause5")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Clause5 {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false, unique = true)
    private Organism organism;

    // 5.1 Validations clause 4
    @Column(name = "validation_enjeux_externes", length = 20)
    @Builder.Default private String validationEnjeuxExternes = "en_attente";

    @Column(name = "validation_enjeux_internes", length = 20)
    @Builder.Default private String validationEnjeuxInternes = "en_attente";

    @Column(name = "validation_parties", length = 20)
    @Builder.Default private String validationParties = "en_attente";

    @Column(name = "validation_perimetre", length = 20)
    @Builder.Default private String validationPerimetre = "en_attente";

    @Column(name = "validation_ressources", length = 20)
    @Builder.Default private String validationRessources = "en_attente";

    @Column(name = "commentaire_enjeux_externes", columnDefinition = "TEXT")
    private String commentaireEnjeuxExternes;

    @Column(name = "commentaire_enjeux_internes", columnDefinition = "TEXT")
    private String commentaireEnjeuxInternes;

    @Column(name = "commentaire_parties", columnDefinition = "TEXT")
    private String commentaireParties;

    @Column(name = "commentaire_perimetre", columnDefinition = "TEXT")
    private String commentairePerimetre;

    @Column(name = "commentaire_ressources", columnDefinition = "TEXT")
    private String commentaireRessources;

    // 5.2 Politique
    @Column(name = "politique_securite_contenu", columnDefinition = "TEXT")
    private String politiqueSecuriteContenu;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "politique_diffusion", columnDefinition = "jsonb")
    @Builder.Default private String politiqueDiffusion = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "objectifs_securite_metier", columnDefinition = "jsonb")
    @Builder.Default private String objectifsSecuriteMetier = "[]";

    // 5.3 Rôles
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exigences_processus", columnDefinition = "jsonb")
    @Builder.Default private String exigencesProcessus = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ressources_smsi", columnDefinition = "jsonb")
    @Builder.Default private String ressourcesSmsi = "[]";

    // 5.4 Indicateurs
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "indicateurs_smsi", columnDefinition = "jsonb")
    @Builder.Default private String indicateursSmsi = "[]";

    @Column(length = 20)
    @Builder.Default private String statut = "en_cours";

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}