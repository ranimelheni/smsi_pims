package com.pims.plateform.repository.ebios;

import com.pims.plateform.entity.ebios.ErmSocieteMission;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ErmSocieteMissionRepository extends JpaRepository<ErmSocieteMission, Long> {
    Optional<ErmSocieteMission> findByAnalyseId(Long analyseId);
}