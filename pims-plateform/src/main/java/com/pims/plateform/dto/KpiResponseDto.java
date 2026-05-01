package com.pims.plateform.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class KpiResponseDto {

    @JsonProperty("organism_id")
    private Long organismId;

    @JsonProperty("organism_nom")
    private String organismNom;

    @JsonProperty("computed_at")
    private LocalDateTime computedAt;

    @JsonProperty("has_data")
    private boolean hasData;

    @JsonProperty("message_vide")
    private String messageVide; // Affiché quand hasData = false

    @JsonProperty("soa")
    private SoaKpiDto soa;

    @JsonProperty("publications")
    private PublicationKpiDto publications;
}