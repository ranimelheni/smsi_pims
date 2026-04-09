package com.pims.plateform.repository;
import com.pims.plateform.entity.SoaControle;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SoaControleRepository extends JpaRepository<SoaControle, Long> {

    List<SoaControle> findBySoa_IdOrderByControleId(Long soaId);

    List<SoaControle> findBySoa_IdAndAnnexe(Long soaId, String annexe);

    Optional<SoaControle> findBySoa_IdAndControleId(Long soaId, String controleId);

    long countBySoa_IdAndInclus(Long soaId, Boolean inclus);

    long countBySoa_IdAndStatutImpl(Long soaId, String statutImpl);
}