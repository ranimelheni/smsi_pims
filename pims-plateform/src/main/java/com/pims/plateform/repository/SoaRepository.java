package com.pims.plateform.repository;
import com.pims.plateform.entity.Soa;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SoaRepository extends JpaRepository<Soa, Long> {
    @EntityGraph(attributePaths = {"organism"})
    Optional<Soa> findByOrganismId(Long orgId);
}