package com.pims.plateform.repository;
import com.pims.plateform.entity.Organigramme;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganigrammeRepository extends JpaRepository<Organigramme, Long> {
    @EntityGraph(attributePaths = {"organism", "uploadedBy"})
    List<Organigramme> findByOrganismIdAndIsActiveTrue(Long orgId);

    @EntityGraph(attributePaths = {"organism", "uploadedBy"})
    Optional<Organigramme> findFirstByOrganismIdAndIsActiveTrueOrderByUploadedAtDesc(Long orgId);
}