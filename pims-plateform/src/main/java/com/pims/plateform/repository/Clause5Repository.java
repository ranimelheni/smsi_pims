package com.pims.plateform.repository;
import com.pims.plateform.entity.Clause5;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface Clause5Repository extends JpaRepository<Clause5, Long> {
    @EntityGraph(attributePaths = {"organism"})
    Optional<Clause5> findByOrganismId(Long orgId);
}