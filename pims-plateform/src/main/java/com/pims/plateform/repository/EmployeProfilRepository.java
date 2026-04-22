package com.pims.plateform.repository;

import com.pims.plateform.entity.EmployeProfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EmployeProfilRepository extends JpaRepository<EmployeProfil, Long> {

    Optional<EmployeProfil> findByUserId(Long userId);

    @Query("SELECT p FROM EmployeProfil p JOIN FETCH p.user JOIN FETCH p.organism WHERE p.organism.id = :orgId ORDER BY p.user.nom")
    List<EmployeProfil> findByOrganismIdWithUser(@Param("orgId") Long orgId);

    @Query("SELECT p FROM EmployeProfil p JOIN FETCH p.user WHERE p.organism.id = :orgId AND p.statutEvaluation = :statut")
    List<EmployeProfil> findByOrganismIdAndStatutEvaluation(@Param("orgId") Long orgId, @Param("statut") String statut);
}