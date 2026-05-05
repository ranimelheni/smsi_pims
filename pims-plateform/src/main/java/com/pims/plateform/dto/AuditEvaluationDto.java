package com.pims.plateform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuditEvaluationDto {
    private Long id;

    @JsonProperty("clause_code")     private String clauseCode;
    @JsonProperty("clause_titre")    private String clauseTitre;
    @JsonProperty("clause_desc")     private String clauseDesc;
    @JsonProperty("parent_code")     private String parentCode;

    private String statut;
    private String justification;

    @JsonProperty("action_planifiee") private String actionPlanifiee;
    private String priorite;
    private String echeance;
    private String responsable;
}