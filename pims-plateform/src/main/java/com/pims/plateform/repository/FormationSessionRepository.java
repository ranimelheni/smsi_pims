package com.pims.plateform.repository;

import com.pims.plateform.entity.FormationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface FormationSessionRepository extends JpaRepository<FormationSession, Long> {

    @Query("SELECT s FROM FormationSession s JOIN FETCH s.createdBy WHERE s.organism.id = :orgId ORDER BY s.dateDebut DESC")
    List<FormationSession> findByOrganismId(@Param("orgId") Long orgId);
}