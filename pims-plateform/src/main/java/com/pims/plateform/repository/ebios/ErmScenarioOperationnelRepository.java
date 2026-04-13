// ErmScenarioOperationnelRepository.java
package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmScenarioOperationnel;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmScenarioOperationnelRepository
        extends JpaRepository<ErmScenarioOperationnel, Long> {

    @EntityGraph(attributePaths = {"scenarioStrategique","bienSupport"})
    List<ErmScenarioOperationnel> findByAnalyseIdOrderByNiveauRisqueDescGraviteDesc(Long analyseId);

    @EntityGraph(attributePaths = {"scenarioStrategique","bienSupport"})
    List<ErmScenarioOperationnel> findByScenarioStrategiqueId(Long ssId);

    long countByAnalyseIdAndNiveauRisque(Long analyseId, Short niveau);
}

