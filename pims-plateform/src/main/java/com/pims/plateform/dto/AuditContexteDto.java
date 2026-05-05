package com.pims.plateform.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuditContexteDto {
    // Organisme
    @JsonProperty("organism_nom")          private String organismNom;
    @JsonProperty("perimetre_smsi")        private String perimetreSmsi;
    @JsonProperty("enjeux_externes")       private Object enjeuxExternes;
    @JsonProperty("enjeux_internes")       private Object enjeuxInternes;
    @JsonProperty("parties_interessees")   private Object partiesInteressees;

    // Clause 6 — Méthodologie risque
    @JsonProperty("methode_risque")        private String methodeRisque;
    @JsonProperty("methode_statut")        private String methodeStatut;
    @JsonProperty("echelle_probabilite")   private Integer echelleProbabilite;
    @JsonProperty("echelle_impact")        private Integer echelleImpact;
    @JsonProperty("seuil_acceptable")      private Integer seuilAcceptable;
    @JsonProperty("seuil_eleve")           private Integer seuilEleve;
    @JsonProperty("labels_probabilite")    private Object labelsProbabilite;
    @JsonProperty("labels_impact")         private Object labelsImpact;

    // SOA
    @JsonProperty("soa_statut")            private String soaStatut;
    @JsonProperty("soa_version")           private String soaVersion;
    @JsonProperty("soa_nb_controles")      private Integer soaNbControles;
    @JsonProperty("soa_nb_implemente")     private Integer soaNbImplemente;
    @JsonProperty("soa_taux")             private Double soaTaux;
}
