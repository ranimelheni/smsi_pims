package com.pims.plateform.repository;

import com.pims.plateform.entity.FicheProcessus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FicheProcessusRepository extends JpaRepository<FicheProcessus, Long> {

    @EntityGraph(attributePaths = {"organism", "pilote", "validePar"})
    Optional<FicheProcessus> findByOrganismIdAndPiloteId(Long organismId, Long piloteId);

    @EntityGraph(attributePaths = {"organism", "pilote", "validePar"})
    List<FicheProcessus> findByOrganismId(Long organismId);

    @EntityGraph(attributePaths = {"organism", "pilote", "validePar"})
    List<FicheProcessus> findByOrganismIdAndStatut(Long organismId, String statut);

    @EntityGraph(attributePaths = {"organism", "pilote", "validePar"})
    List<FicheProcessus> findByOrganismIdAndStatutIn(Long organismId, List<String> statuts);

    @EntityGraph(attributePaths = {"organism", "pilote", "validePar"})
    List<FicheProcessus> findByPiloteId(Long piloteId);

    @EntityGraph(attributePaths = {"organism", "pilote", "validePar"})
    Optional<FicheProcessus> findById(Long id);
}