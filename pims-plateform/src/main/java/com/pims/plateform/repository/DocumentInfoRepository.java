package com.pims.plateform.repository;

import com.pims.plateform.entity.DocumentInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface DocumentInfoRepository extends JpaRepository<DocumentInfo, Long> {

    @Query("SELECT d FROM DocumentInfo d JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.fiche WHERE d.organism.id = :orgId ORDER BY d.createdAt DESC")
    List<DocumentInfo> findByOrganismId(@Param("orgId") Long orgId);

    @Query("SELECT d FROM DocumentInfo d JOIN FETCH d.uploadedBy WHERE d.fiche.id = :ficheId")
    List<DocumentInfo> findByFicheId(@Param("ficheId") Long ficheId);
    @Query("SELECT d FROM DocumentInfo d JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.fiche " +
       "WHERE d.organism.id = :orgId AND d.uploadedBy.id = :userId ORDER BY d.createdAt DESC")
List<DocumentInfo> findByOrganismIdAndUploadedById(
    @Param("orgId") Long orgId, @Param("userId") Long userId);
}