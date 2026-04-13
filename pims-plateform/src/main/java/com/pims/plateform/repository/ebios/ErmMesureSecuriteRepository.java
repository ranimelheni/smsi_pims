package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmMesureSecurite;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmMesureSecuriteRepository
        extends JpaRepository<ErmMesureSecurite, Long> {

    List<ErmMesureSecurite> findByAnalyseIdOrderByEcheanceMoisAsc(Long analyseId);
    long countByAnalyseIdAndStatut(Long analyseId, String statut);
}