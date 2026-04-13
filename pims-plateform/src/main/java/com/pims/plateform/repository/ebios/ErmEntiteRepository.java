package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmEntite;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmEntiteRepository extends JpaRepository<ErmEntite, Long> {
    List<ErmEntite> findByAnalyseIdOrderByNomEntite(Long analyseId);
}