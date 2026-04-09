package com.pims.plateform.repository;
import com.pims.plateform.entity.ModificationSmsi;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ModificationSmsiRepository extends JpaRepository<ModificationSmsi, Long> {
    @EntityGraph(attributePaths = {"organism", "declareBy"})
    List<ModificationSmsi> findByOrganismIdOrderByCreatedAtDesc(Long orgId);
}