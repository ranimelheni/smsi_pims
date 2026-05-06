package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "suivi_nc",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"audit_session_id", "audit_evaluation_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SuiviNc {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_session_id", nullable = false)
    private AuditSession auditSession;

    @Column(name = "audit_evaluation_id", nullable = false)
    private Long auditEvaluationId;

    // Copie depuis AuditEvaluation
    @Column(name = "clause_code",   nullable = false, length = 20)
    private String clauseCode;

    @Column(name = "clause_titre")
    private String clauseTitre;

    @Column(name = "statut_audit",  nullable = false, length = 30)
    private String statutAudit;      // non_conforme | partiel

    @Column(name = "justification", columnDefinition = "TEXT")
    private String justification;

    @Column(name = "action_planifiee", columnDefinition = "TEXT")
    private String actionPlanifiee;

    @Column(length = 20) @Builder.Default
    private String priorite = "normale";

    @Column(name = "echeance_audit")
    private LocalDate echeanceAudit;

    @Column(name = "responsable_audit")
    private String responsableAudit;

    // Évaluation RSSI
    @Column(name = "statut_impl", length = 30) @Builder.Default
    private String statutImpl = "non_traite";

    @Column(name = "responsable_rssi")
    private String responsableRssi;

    @Column(name = "echeance_rssi")
    private LocalDate echeanceRssi;

    @Column(name = "commentaire_rssi", columnDefinition = "TEXT")
    private String commentaireRssi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evalue_par_id")
    private User evaluePar;

    @Column(name = "evalue_at")
    private LocalDateTime evalueAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}