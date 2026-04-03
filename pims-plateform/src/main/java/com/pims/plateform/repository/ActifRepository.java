package com.pims.plateform.repository;

import com.pims.plateform.entity.Actif;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActifRepository extends JpaRepository<Actif, Long> {

    @EntityGraph(attributePaths = {"organism", "validePar"})
    List<Actif> findByOrganismIdOrderByCreatedAtDesc(Long orgId);

    @EntityGraph(attributePaths = {"organism", "validePar"})
    List<Actif> findByOrganismIdAndStatut(Long orgId, String statut);

    @EntityGraph(attributePaths = {"organism", "validePar"})
    List<Actif> findByOrganismIdAndStatutIn(Long orgId, List<String> statuts);

    @EntityGraph(attributePaths = {"organism", "validePar"})
    List<Actif> findByOrganismIdAndCategorie(Long orgId, String categorie);

    boolean existsByOrganismIdAndNomAndCategorie(Long orgId, String nom, String categorie);

    @Query("""
        SELECT COUNT(a) FROM Actif a
        WHERE a.organism.id = :orgId AND a.statut = 'actif'
    """)
    long countActifsValides(@Param("orgId") Long orgId);

    @Query("""
        SELECT COUNT(a) FROM Actif a
        WHERE a.organism.id = :orgId AND a.statut = 'en_attente_validation'
    """)
    long countActifsEnAttente(@Param("orgId") Long orgId);
}