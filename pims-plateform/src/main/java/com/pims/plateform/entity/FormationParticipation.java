package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "formation_participation",
       uniqueConstraints = @UniqueConstraint(columnNames = {"session_id","employe_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FormationParticipation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private FormationSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", nullable = false)
    private User employe;

    @Column(length = 30)
    @Builder.Default
    private String statut = "inscrit";

    @Column(name = "presence_confirmee")
    @Builder.Default
    private Boolean presenceConfirmee = false;

    @Column(name = "score_evaluation", precision = 5, scale = 2)
    private BigDecimal scoreEvaluation;

    @Column(name = "commentaire_rssi", columnDefinition = "TEXT")
    private String commentaireRssi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evalue_par")
    private User evaluePar;

    @Column(name = "evalue_at")
    private LocalDateTime evalueAt;

    @Column(name = "inscrit_at")
    private LocalDateTime inscritAt;

    @PrePersist protected void onCreate() { inscritAt = LocalDateTime.now(); }
}