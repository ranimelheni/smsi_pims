package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmSocleSecurite;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmSocleSecuriteRepository extends JpaRepository<ErmSocleSecurite, Long> {
    List<ErmSocleSecurite> findByAnalyseIdOrderByNomReferentiel(Long analyseId);
}