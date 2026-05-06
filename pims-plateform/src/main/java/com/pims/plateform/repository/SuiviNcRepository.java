package com.pims.plateform.repository;

import com.pims.plateform.entity.SuiviNc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SuiviNcRepository extends JpaRepository<SuiviNc, Long> {

    @Query("""
        SELECT s FROM SuiviNc s
        JOIN FETCH s.auditSession
        WHERE s.organism.id = :orgId
        ORDER BY
          CASE s.statutImpl
            WHEN 'non_traite' THEN 1
            WHEN 'en_cours'   THEN 2
            WHEN 'reporte'    THEN 3
            WHEN 'fait'       THEN 4
            WHEN 'accepte'    THEN 5
            ELSE 6
          END,
          s.priorite DESC
        """)
    List<SuiviNc> findByOrganismId(@Param("orgId") Long orgId);

    boolean existsByAuditSessionIdAndAuditEvaluationId(
        Long sessionId, Long evaluationId);
}