package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmSourceRisque;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmSourceRisqueRepository extends JpaRepository<ErmSourceRisque, Long> {
    List<ErmSourceRisque> findByAnalyseIdOrderByLibelle(Long analyseId);
    List<ErmSourceRisque> findByAnalyseIdAndRetenuTrue(Long analyseId);
    long countByAnalyseIdAndRetenuTrue(Long analyseId);
}