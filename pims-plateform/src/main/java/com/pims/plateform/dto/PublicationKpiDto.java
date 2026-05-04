package com.pims.plateform.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PublicationKpiDto {

    private boolean hasData;

    // ── Chiffres globaux ─────────────────────────────────────
    private int    totalPublications;
    private int    totalPubliees;
    private int    totalUsersActifs;
    private int    nbLecteursUniques;
    private int    totalLectures;
    private double tauxLectureGlobal;

    // ── Bar chart : une entrée par publication ───────────────
    private List<ParPublicationDto> parPublication;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParPublicationDto {
        private Long   publicationId;
        private String titre;
        private String type;
        private String priorite;
        private String publieLe;       // "YYYY-MM-DD"
        private int    nbLecteurs;
        private int    totalUsersActifs;
        private double tauxLecture;    // 0–100
    }
}