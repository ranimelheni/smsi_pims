package com.pims.plateform.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


/**
 * DTO retourné au frontend pour une fiche processus.
 * Les champs JSONB sont désérialisés en Object (List / Map) pour le client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FicheProcessusDto {

    private Long id;

    @JsonProperty("organism_id")
    private Long organismId;

    @JsonProperty("audit_type")
    private String auditType;

    // Section 1
    private String intitule;
    private String code;

    @JsonProperty("type_processus")
    private String typeProcessus;

    private String domaine;
    private String activites;
    private String version;

    // Section 2
    private String finalite;
    private Object beneficiaires;

    // Section 3
    private Object declencheurs;

    // Section 4
    @JsonProperty("elements_entree")
    private Object elementsEntree;

    @JsonProperty("elements_sortie_intentionnels")
    private Object elementsSortieIntentionnels;

    @JsonProperty("elements_sortie_non_intentionnels")
    private Object elementsSortieNonIntentionnels;

    // Section 5
    @JsonProperty("informations_documentees")
    private Object informationsDocumentees;

    // Section 6
    @JsonProperty("contraintes_reglementaires")
    private Object contraintesReglementaires;

    @JsonProperty("contraintes_internes")
    private String contraintesInternes;

    @JsonProperty("contraintes_temporelles")
    private String contraintesTemporelles;

    @JsonProperty("contraintes_techniques")
    private String contraintegTechniques;

    // Section 7
    @JsonProperty("pilote_id")
    private Long piloteId;

    @JsonProperty("pilote_nom")
    private String piloteNom;

    private Object acteurs;
    private Object ressources;

    // Section 8
    @JsonProperty("objectifs_kpi")
    private Object objectifsKpi;

    // Section 9
    @JsonProperty("moyens_surveillance")
    private Object moyensSurveillance;

    @JsonProperty("moyens_mesure")
    private Object moyensMesure;

    // Section 10
    private Object interactions;

    // Section 11
    private Object risques;

    @JsonProperty("note_max")
    private Integer noteMax;

    @JsonProperty("risque_dominant")
    private String risqueDominant;

    // Section 12
    private Object opportunites;

    // DPO
    @JsonProperty("data_dpo")
    private Object dataDpo;

    // Workflow
    private String statut;

    @JsonProperty("soumis_at")
    private String soumisAt;

    @JsonProperty("valide_at")
    private String valideAt;

    @JsonProperty("valide_by")
    private Long valideBy;

    @JsonProperty("commentaire_rejet")
    private String commentaireRejet;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}