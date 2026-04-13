package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmObjectifVise;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmObjectifViseRepository extends JpaRepository<ErmObjectifVise, Long> {
    @EntityGraph(attributePaths = {"source"})
    List<ErmObjectifVise> findByAnalyseIdOrderBySourceIdAscLibelleAsc(Long analyseId);

    @EntityGraph(attributePaths = {"source"})
    List<ErmObjectifVise> findBySourceIdOrderByLibelle(Long sourceId);

    @EntityGraph(attributePaths = {"source"})
    List<ErmObjectifVise> findByAnalyseIdAndRetenuTrue(Long analyseId);

    long countByAnalyseIdAndRetenuTrue(Long analyseId);
}