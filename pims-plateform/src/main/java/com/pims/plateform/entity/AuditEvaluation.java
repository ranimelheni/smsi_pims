package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_evaluation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id","clause_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvaluation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AuditSession session;

    @Column(name = "clause_code", nullable = false, length = 20)
    private String clauseCode;

    @Column(name = "clause_titre")
    private String clauseTitre;

    @Column(length = 30)
    @Builder.Default
    private String statut = "non_evalue";

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(name = "action_planifiee", columnDefinition = "TEXT")
    private String actionPlanifiee;

    @Column(length = 20)
    @Builder.Default
    private String priorite = "normale";

    private java.time.LocalDate echeance;

    private String responsable;
}