package com.pims.plateform.repository;

import com.pims.plateform.entity.AuditEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AuditEvaluationRepository extends JpaRepository<AuditEvaluation, Long> {

    @Query("SELECT e FROM AuditEvaluation e WHERE e.session.id = :sessionId ORDER BY e.clauseCode")
    List<AuditEvaluation> findBySessionId(@Param("sessionId") Long sessionId);

    Optional<AuditEvaluation> findBySessionIdAndClauseCode(Long sessionId, String clauseCode);

    @Query("SELECT COUNT(e) FROM AuditEvaluation e WHERE e.session.id = :sid AND e.statut = :s")
    long countBySessionIdAndStatut(@Param("sid") Long sessionId, @Param("s") String statut);
}