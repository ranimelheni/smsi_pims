package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmAnalyse;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ErmAnalyseRepository extends JpaRepository<ErmAnalyse, Long> {
    @EntityGraph(attributePaths = {"organism","createdBy"})
    Optional<ErmAnalyse> findByOrganismId(Long orgId);
}