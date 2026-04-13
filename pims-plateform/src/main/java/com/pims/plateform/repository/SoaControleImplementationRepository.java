package com.pims.plateform.repository;

import com.pims.plateform.entity.SoaControleImplementation;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SoaControleImplementationRepository
        extends JpaRepository<SoaControleImplementation, Long> {

    @EntityGraph(attributePaths = {"soaControle","organism"})
    Optional<SoaControleImplementation> findBySoaControleId(Long controleId);

    @EntityGraph(attributePaths = {"soaControle"})
    List<SoaControleImplementation> findByOrganismId(Long orgId);

    long countByOrganismIdAndStatutDetail(Long orgId, String statut);

    @Query("SELECT i FROM SoaControleImplementation i " +
           "JOIN i.soaControle c " +
           "WHERE c.soa.id = :soaId")
    List<SoaControleImplementation> findBySoaId(@org.springframework.data.repository.query.Param("soaId") Long soaId);
}