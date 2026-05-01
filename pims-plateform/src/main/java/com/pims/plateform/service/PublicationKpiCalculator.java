package com.pims.plateform.service;
import com.pims.plateform.dto.PublicationKpiDto;
import com.pims.plateform.repository.KpiQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicationKpiCalculator {

    private final KpiQueryRepository repo;

    public PublicationKpiDto calculate(Long orgId, int periodeEvolutionMois) {

        if (!repo.hasPublicationData(orgId)) {
            return PublicationKpiDto.builder()
                .hasData(false)
                .totalPublications(0)
                .parType(List.of())
                .parPriorite(List.of())
                .evolutionMensuelle(List.of())
                .build();
        }

        // Global
        Optional<Map<String, Object>> globalOpt = repo.getPublicationGlobal(orgId);
        if (globalOpt.isEmpty()) {
            return PublicationKpiDto.builder()
                .hasData(false)
                .parType(List.of())
                .parPriorite(List.of())
                .evolutionMensuelle(List.of())
                .build();
        }

        Map<String, Object> g = globalOpt.get();
        int total        = toInt(g.get("total_publications"));
        int destinataires= toInt(g.get("total_destinataires"));
        int lecteurs     = toInt(g.get("nb_lecteurs_uniques"));
        double taux      = safe(toDouble(g.get("taux_lecture_utilisateurs")));

        // Par type → radar
        List<PublicationKpiDto.ParTypeDto> parType = repo
            .getPublicationParType(orgId).stream()
            .map(row -> PublicationKpiDto.ParTypeDto.builder()
                .typePublication(str(row.get("type_publication")))
                .nbPublications(toInt(row.get("nb_publications")))
                .nbLectures(toInt(row.get("nb_lectures")))
                .tauxLecture(safe(toDouble(row.get("taux_lecture"))))
                .build()
            ).collect(Collectors.toList());

        // Par priorité → radar
        List<PublicationKpiDto.ParPrioriteDto> parPriorite = repo
            .getPublicationParPriorite(orgId).stream()
            .map(row -> PublicationKpiDto.ParPrioriteDto.builder()
                .priorite(str(row.get("priorite")))
                .nbPublications(toInt(row.get("nb_publications")))
                .nbLecteurs(toInt(row.get("nb_lecteurs")))
                .tauxLecture(safe(toDouble(row.get("taux_lecture"))))
                .build()
            ).collect(Collectors.toList());

        // Évolution mensuelle → line chart
        List<PublicationKpiDto.EvolutionMensuelleDto> evolution = repo
            .getPublicationEvolutionMensuelle(orgId, periodeEvolutionMois).stream()
            .map(row -> PublicationKpiDto.EvolutionMensuelleDto.builder()
                .mois(str(row.get("mois")))
                .nbPublications(toInt(row.get("nb_publications")))
                .nbLectures(toInt(row.get("nb_lectures")))
                .nbLecteursUniques(toInt(row.get("nb_lecteurs_uniques")))
                .build()
            ).collect(Collectors.toList());

        return PublicationKpiDto.builder()
            .hasData(total > 0)
            .totalPublications(total)
            .totalDestinataires(destinataires)
            .nbLecteursUniques(lecteurs)
            .tauxLectureGlobal(taux)
            .parType(parType)
            .parPriorite(parPriorite)
            .evolutionMensuelle(evolution)
            .build();
    }

    private double safe(double v) {
        return (Double.isNaN(v) || Double.isInfinite(v)) ? 0.0 : v;
    }
    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); }
        catch (Exception e) { return 0.0; }
    }
    private int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); }
        catch (Exception e) { return 0; }
    }
    private String str(Object v) { return v != null ? v.toString() : ""; }
}