package com.pims.plateform.repository;

import com.pims.plateform.entity.FormationParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FormationParticipationRepository extends JpaRepository<FormationParticipation, Long> {

    @Query("SELECT p FROM FormationParticipation p JOIN FETCH p.employe WHERE p.session.id = :sessionId")
    List<FormationParticipation> findBySessionId(@Param("sessionId") Long sessionId);

    List<FormationParticipation> findByEmployeId(Long employeId);

    Optional<FormationParticipation> findBySessionIdAndEmployeId(Long sessionId, Long employeId);
}