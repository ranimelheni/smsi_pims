package com.pims.plateform.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class KpiDashboardDto {

    private Long          organismId;
    private String        organismNom;
    private String        roleUtilisateur;
    private LocalDateTime computedAt;
    private boolean       fromCache;

    private List<KpiDto>  kpis;

    // Détail SOA (RSSI/direction/auditeur)
    private SoaDetailDto  soaDetail;

    // Historique 30 jours (RSSI/direction)
    private List<KpiHistoriquePointDto> historique;

    @Data @Builder
    public static class SoaDetailDto {
        private int totalInclus;
        private int nbImplemente;
        private int a5Inclus;
        private int a6Inclus;
        private int a7Inclus;
        private int a8Inclus;
    }

    @Data @Builder
    public static class KpiHistoriquePointDto {
        private String date;
        private double employe_evalue;
        private double participation_formation;
        private double lecture_publication;
        private double documents_valides;
        private double conformite_soa;
    }
}