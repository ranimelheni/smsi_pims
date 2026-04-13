package com.pims.plateform.repository;

import com.pims.plateform.entity.RegistreTraitement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistreTraitementRepository extends JpaRepository<RegistreTraitement, Long> {

    @Query("""
        SELECT r FROM RegistreTraitement r
        JOIN FETCH r.createdBy
        JOIN FETCH r.organism
        LEFT JOIN FETCH r.ficheProcessus
        WHERE r.organism.id = :orgId
        ORDER BY r.service ASC
    """)
    List<RegistreTraitement> findByOrganismIdOrderByServiceAsc(@Param("orgId") Long orgId);

    @Query("""
        SELECT r FROM RegistreTraitement r
        JOIN FETCH r.createdBy
        JOIN FETCH r.organism
        LEFT JOIN FETCH r.ficheProcessus
        WHERE r.organism.id = :orgId
        AND r.ficheProcessus IS NOT NULL
    """)
    List<RegistreTraitement> findByOrganismIdWithFiche(@Param("orgId") Long orgId);

    long countByOrganismIdAndStatut(Long organismId, String statut);
    long countByOrganismIdAndPiaRequis(Long organismId, Boolean piaRequis);
    long countByOrganismIdAndAnalysePia(Long organismId, Boolean analysePia);

    Optional<RegistreTraitement> findByFicheProcessusId(Long ficheProcessusId);
    boolean existsByFicheProcessusId(Long ficheProcessusId);
}