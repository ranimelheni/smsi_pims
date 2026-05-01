package com.pims.plateform.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SoaKpiDto {

    @JsonProperty("has_data")
    private boolean hasData;

    @JsonProperty("taux_global")
    private double tauxGlobal;

    @JsonProperty("total_controles")
    private int totalControles;

    @JsonProperty("nb_implemente")
    private int nbImplemente;

    @JsonProperty("nb_en_cours")
    private int nbEnCours;

    @JsonProperty("nb_non_commence")
    private int nbNonCommence;

    // Pour radar chart — un point par annexe
    @JsonProperty("par_annexe")
    private List<AnnexeDto> parAnnexe;

    // Pour line chart — évolution dans le temps
    @JsonProperty("evolution")
    private List<EvolutionPointDto> evolution;

    @Data
    @Builder
    public static class AnnexeDto {
        private String annexe;

        @JsonProperty("annexe_label")
        private String annexeLabel;

        @JsonProperty("total_inclus")
        private int totalInclus;

        @JsonProperty("nb_implemente")
        private int nbImplemente;

        @JsonProperty("taux_annexe")
        private double tauxAnnexe;
    }

    @Data
    @Builder
    public static class EvolutionPointDto {
        private String date;

        @JsonProperty("taux_soa")
        private double tauxSoa;

        @JsonProperty("nb_implemente")
        private int nbImplemente;

        @JsonProperty("total_inclus")
        private int totalInclus;
    }
}