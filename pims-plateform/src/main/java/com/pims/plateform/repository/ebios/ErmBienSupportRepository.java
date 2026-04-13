package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmBienSupport;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmBienSupportRepository extends JpaRepository<ErmBienSupport, Long> {
    @EntityGraph(attributePaths = {"valeurMetier","entite"})
    List<ErmBienSupport> findByAnalyseIdOrderByDenomination(Long analyseId);

    List<ErmBienSupport> findByValeurMetierId(Long vmId);
}