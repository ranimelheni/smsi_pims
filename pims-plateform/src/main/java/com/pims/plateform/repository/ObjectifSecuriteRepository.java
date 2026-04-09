package com.pims.plateform.repository;
import com.pims.plateform.entity.ObjectifSecurite;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ObjectifSecuriteRepository extends JpaRepository<ObjectifSecurite, Long> {
    @EntityGraph(attributePaths = {"organism"})
    List<ObjectifSecurite> findByOrganismIdOrderByEcheanceAsc(Long orgId);
    long countByOrganismIdAndStatut(Long orgId, String statut);
}