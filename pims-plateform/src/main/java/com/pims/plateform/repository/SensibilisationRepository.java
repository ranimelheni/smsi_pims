package com.pims.plateform.repository;
import com.pims.plateform.entity.Sensibilisation;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SensibilisationRepository extends JpaRepository<Sensibilisation, Long> {
    @EntityGraph(attributePaths = {"acteur", "organism"})
    List<Sensibilisation> findByOrganismIdOrderByDateEcheanceAsc(Long orgId);

    @EntityGraph(attributePaths = {"acteur", "organism"})
    List<Sensibilisation> findByOrganismIdAndActeurId(Long orgId, Long acteurId);

    long countByOrganismIdAndStatut(Long orgId, String statut);
}