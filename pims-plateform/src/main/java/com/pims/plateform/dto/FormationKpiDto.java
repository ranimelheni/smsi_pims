package com.pims.plateform.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FormationKpiDto {

    private boolean hasData;

    // ── Global ───────────────────────────────────────────────
    private int    totalSessions;
    private int    sessionsTerminees;
    private int    sessionsPlanifiees;
    private int    sessionsAnnulees;
    private int    totalInscriptions;
    private int    totalPresents;
    private double tauxParticipationGlobal;   // 0–100

    // ── Bar chart : une barre par session ────────────────────
    private List<ParSessionDto> parSession;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParSessionDto {
        private Long   sessionId;
        private String titre;
        private String type;
        private String statut;
        private String dateSession;        // "YYYY-MM-DD"
        private Integer maxParticipants;
        private int    nbInscrits;
        private int    nbPresents;
        private double tauxParticipation;  // 0–100
    }
}