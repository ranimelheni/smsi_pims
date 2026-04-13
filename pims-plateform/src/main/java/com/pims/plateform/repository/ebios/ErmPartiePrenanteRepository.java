package com.pims.plateform.repository.ebios;
import com.pims.plateform.entity.ebios.ErmPartiePrenante;

import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmPartiePrenanteRepository extends JpaRepository<ErmPartiePrenante, Long> {
    List<ErmPartiePrenante> findByAnalyseIdOrderByLibelle(Long analyseId);
    List<ErmPartiePrenante> findByAnalyseIdAndRetenuTrue(Long analyseId);
}