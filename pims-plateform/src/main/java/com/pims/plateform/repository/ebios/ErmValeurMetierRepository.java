package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmValeurMetier;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmValeurMetierRepository extends JpaRepository<ErmValeurMetier, Long> {
    @EntityGraph(attributePaths = {"entite"})
    List<ErmValeurMetier> findByAnalyseIdOrderByDenomination(Long analyseId);
}