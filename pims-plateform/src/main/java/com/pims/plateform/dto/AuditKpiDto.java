package com.pims.plateform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class AuditKpiDto {
    @JsonProperty("session_id")                  private Long sessionId;
    @JsonProperty("total_clauses_evaluees")      private int totalClausesEvaluees;
    @JsonProperty("nb_conforme")                 private int nbConforme;
    @JsonProperty("nb_non_conforme")             private int nbNonConforme;
    @JsonProperty("nb_partiel")                  private int nbPartiel;
    @JsonProperty("nb_planifie")                 private int nbPlanifie;
    @JsonProperty("nb_en_cours")                 private int nbEnCours;
    @JsonProperty("nb_non_applicable")           private int nbNonApplicable;
    @JsonProperty("nb_non_evalue")               private int nbNonEvalue;
    @JsonProperty("taux_conformite_globale")     private double tauxConformiteGlobale;
    @JsonProperty("taux_conformite_partielle")   private double tauxConformitePartielle;

    // Répartition par clause principale pour radar
    @JsonProperty("par_clause")
    private List<ClauseKpiDto> parClause;

    @Data @Builder
    public static class ClauseKpiDto {
        @JsonProperty("clause_principale")  private String clausePrincipale;
        private int total;
        @JsonProperty("nb_conforme")        private int nbConforme;
        @JsonProperty("nb_non_conforme")    private int nbNonConforme;
        @JsonProperty("nb_partiel")         private int nbPartiel;
        @JsonProperty("taux_clause")        private double tauxClause;
    }
}