package com.pims.plateform.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PublicationKpiDto {

    @JsonProperty("has_data")
    private boolean hasData;

    @JsonProperty("total_publications")
    private int totalPublications;

    @JsonProperty("total_destinataires")
    private int totalDestinataires;

    @JsonProperty("nb_lecteurs_uniques")
    private int nbLecteursUniques;

    @JsonProperty("taux_lecture_global")
    private double tauxLectureGlobal;

    // Pour radar chart — par type
    @JsonProperty("par_type")
    private List<ParTypeDto> parType;

    // Pour radar chart — par priorité
    @JsonProperty("par_priorite")
    private List<ParPrioriteDto> parPriorite;

    // Pour line chart — évolution mensuelle
    @JsonProperty("evolution_mensuelle")
    private List<EvolutionMensuelleDto> evolutionMensuelle;

    @Data
    @Builder
    public static class ParTypeDto {
        @JsonProperty("type_publication")
        private String typePublication;

        @JsonProperty("nb_publications")
        private int nbPublications;

        @JsonProperty("nb_lectures")
        private int nbLectures;

        @JsonProperty("taux_lecture")
        private double tauxLecture;
    }

    @Data
    @Builder
    public static class ParPrioriteDto {
        private String priorite;

        @JsonProperty("nb_publications")
        private int nbPublications;

        @JsonProperty("nb_lecteurs")
        private int nbLecteurs;

        @JsonProperty("taux_lecture")
        private double tauxLecture;
    }

    @Data
    @Builder
    public static class EvolutionMensuelleDto {
        private String mois;

        @JsonProperty("nb_publications")
        private int nbPublications;

        @JsonProperty("nb_lectures")
        private int nbLectures;

        @JsonProperty("nb_lecteurs_uniques")
        private int nbLecteursUniques;
    }
}