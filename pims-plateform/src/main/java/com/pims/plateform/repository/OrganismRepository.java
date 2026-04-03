package com.pims.plateform.repository;

import com.pims.plateform.entity.Organism;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganismRepository extends JpaRepository<Organism, Long> {

    List<Organism> findAllByOrderByCreatedAtDesc();

    List<Organism> findByIsActiveTrue();
    @Query("SELECT COUNT(u) FROM User u WHERE u.organism.id = :organismId")
    long countUsersByOrganismId(@Param("organismId") Long organismId);
}