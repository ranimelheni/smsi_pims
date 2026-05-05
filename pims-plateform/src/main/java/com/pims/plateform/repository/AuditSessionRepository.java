package com.pims.plateform.repository;

import com.pims.plateform.entity.AuditSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AuditSessionRepository extends JpaRepository<AuditSession, Long> {

    @Query("SELECT s FROM AuditSession s JOIN FETCH s.auditeur WHERE s.organism.id = :orgId ORDER BY s.createdAt DESC")
    List<AuditSession> findByOrganismId(@Param("orgId") Long orgId);

    @Query("SELECT s FROM AuditSession s WHERE s.organism.id = :orgId AND s.auditeur.id = :userId ORDER BY s.createdAt DESC")
    List<AuditSession> findByOrganismIdAndAuditeurId(@Param("orgId") Long orgId, @Param("userId") Long userId);

    Optional<AuditSession> findTopByOrganismIdOrderByCreatedAtDesc(Long orgId);
}