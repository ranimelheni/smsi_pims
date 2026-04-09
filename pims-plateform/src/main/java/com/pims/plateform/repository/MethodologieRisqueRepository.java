package com.pims.plateform.repository;

import com.pims.plateform.entity.MethodologieRisque;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MethodologieRisqueRepository extends JpaRepository<MethodologieRisque, Long> {

    @EntityGraph(attributePaths = {"organism", "validePar", "proposePar"})
    Optional<MethodologieRisque> findByOrganismId(Long orgId);
}