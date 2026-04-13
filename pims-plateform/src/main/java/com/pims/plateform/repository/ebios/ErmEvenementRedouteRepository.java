package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmEvenementRedoute;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmEvenementRedouteRepository extends JpaRepository<ErmEvenementRedoute, Long> {
    @EntityGraph(attributePaths = {"valeurMetier"})
    List<ErmEvenementRedoute> findByAnalyseIdOrderByGraviteDesc(Long analyseId);
    List<ErmEvenementRedoute> findByValeurMetierId(Long vmId);
}