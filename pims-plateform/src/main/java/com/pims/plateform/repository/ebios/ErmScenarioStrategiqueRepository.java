package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmScenarioStrategique;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmScenarioStrategiqueRepository extends JpaRepository<ErmScenarioStrategique, Long> {
    @EntityGraph(attributePaths = {"source","objectif","evenement"})
    List<ErmScenarioStrategique> findByAnalyseIdOrderByNiveauRisqueDescGraviteDesc(Long analyseId);

    @EntityGraph(attributePaths = {"source","objectif","evenement"})
    List<ErmScenarioStrategique> findByAnalyseIdAndDecisionTraitement(Long analyseId, String decision);

    long countByAnalyseIdAndNiveauRisque(Long analyseId, Short niveau);
}