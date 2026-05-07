package com.pims.plateform.repository;

import com.pims.plateform.entity.SuiviNc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SuiviNcRepository extends JpaRepository<SuiviNc, Long> {

    // Liste toutes les NC de l'organisme (pour récupérer les sessions dispo)
    @Query("""
        SELECT s FROM SuiviNc s
        JOIN FETCH s.auditSession
        WHERE s.organism.id = :orgId
        ORDER BY s.auditSession.dateDebut DESC, s.priorite DESC
        """)
    List<SuiviNc> findByOrganismId(@Param("orgId") Long orgId);

    // Liste NC d'une session spécifique
    @Query("""
        SELECT s FROM SuiviNc s
        JOIN FETCH s.auditSession
        WHERE s.organism.id = :orgId
          AND s.auditSession.id = :sessionId
        ORDER BY
          CASE s.statutImpl
            WHEN 'non_traite' THEN 1
            WHEN 'en_cours'   THEN 2
            WHEN 'reporte'    THEN 3
            WHEN 'fait'       THEN 4
            WHEN 'accepte'    THEN 5
            ELSE 6
          END, s.priorite DESC
        """)
    List<SuiviNc> findByOrganismIdAndSessionId(
        @Param("orgId") Long orgId,
        @Param("sessionId") Long sessionId);

    boolean existsByAuditSessionIdAndAuditEvaluationId(
        Long sessionId, Long evaluationId);
}