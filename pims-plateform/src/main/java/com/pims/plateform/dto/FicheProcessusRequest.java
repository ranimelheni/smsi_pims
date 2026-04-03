package com.pims.plateform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Corps de la requête PUT /api/fiches/{id}.
 * Tous les champs sont optionnels (null = non modifié).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FicheProcessusRequest {

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
}