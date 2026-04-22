package com.pims.plateform.repository;

import com.pims.plateform.entity.CommunicationPublication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommunicationPublicationRepository extends JpaRepository<CommunicationPublication, Long> {

    @Query("""
        SELECT p FROM CommunicationPublication p JOIN FETCH p.publiePar
        WHERE p.organism.id = :orgId AND p.estPublie = true
        AND (p.cible = 'tous' OR p.cible = :role)
        ORDER BY p.estEpingle DESC, p.publieAt DESC
    """)
    List<CommunicationPublication> findVisibleForRole(
        @Param("orgId") Long orgId, @Param("role") String role);

    @Query("SELECT p FROM CommunicationPublication p JOIN FETCH p.publiePar WHERE p.organism.id = :orgId ORDER BY p.createdAt DESC")
    List<CommunicationPublication> findAllByOrganismId(@Param("orgId") Long orgId);
}