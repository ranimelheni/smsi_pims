package com.pims.plateform.repository;

import com.pims.plateform.entity.FicheTechnique;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FicheTechniqueRepository extends JpaRepository<FicheTechnique, Long> {

    @EntityGraph(attributePaths = {"organism", "acteur", "validePar"})
    Optional<FicheTechnique> findByOrganismIdAndActeurId(Long orgId, Long acteurId);

    @EntityGraph(attributePaths = {"organism", "acteur", "validePar"})
    List<FicheTechnique> findByOrganismId(Long orgId);

    @EntityGraph(attributePaths = {"organism", "acteur", "validePar"})
    List<FicheTechnique> findByOrganismIdAndStatut(Long orgId, String statut);

    @EntityGraph(attributePaths = {"organism", "acteur", "validePar"})
    List<FicheTechnique> findByOrganismIdAndStatutIn(Long orgId, List<String> statuts);

    @EntityGraph(attributePaths = {"organism", "acteur", "validePar"})
    Optional<FicheTechnique> findById(Long id);
}