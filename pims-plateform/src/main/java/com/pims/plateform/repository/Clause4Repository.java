package com.pims.plateform.repository;

import com.pims.plateform.entity.Clause4;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Clause4Repository extends JpaRepository<Clause4, Long> {

    @EntityGraph(attributePaths = {"organism"})
    Optional<Clause4> findByOrganismId(Long organismId);
}