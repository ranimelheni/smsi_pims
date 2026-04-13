package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmRisqueResiduel;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ErmRisqueResiduelRepository
        extends JpaRepository<ErmRisqueResiduel, Long> {

    @EntityGraph(attributePaths = {"scenarioStrategique"})
    List<ErmRisqueResiduel> findByAnalyseIdOrderByNiveauRisqueResiduelDesc(Long analyseId);

    Optional<ErmRisqueResiduel> findByAnalyseIdAndScenarioStrategiqueId(
            Long analyseId, Long ssId);
}