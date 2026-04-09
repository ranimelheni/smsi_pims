package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "soa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Soa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false, unique = true)
    private Organism organism;

    @Column(name = "audit_type", nullable = false, length = 20)
    @Builder.Default private String auditType = "iso27001";

    @Column(name = "role_organisme", length = 30)
    @Builder.Default private String roleOrganisme = "responsable";

    @Column(length = 10)  @Builder.Default private String version = "v1.0";
    @Column(length = 20)  @Builder.Default private String statut  = "brouillon";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_by")
    private User validePar;

    @Column(name = "valide_at") private LocalDateTime valideAt;

    @Column(name = "commentaire_direction", columnDefinition = "TEXT")
    private String commentaireDirection;

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}