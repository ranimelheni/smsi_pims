package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmActionElementaire;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ErmActionElementaireRepository
        extends JpaRepository<ErmActionElementaire, Long> {

    List<ErmActionElementaire> findByScenarioOpIdOrderByNumero(Long soId);
    long countByAnalyseId(Long analyseId);
}