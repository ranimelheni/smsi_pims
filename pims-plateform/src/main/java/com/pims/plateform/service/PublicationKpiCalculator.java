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

    public PublicationKpiDto calculate(Long orgId, int periodeJours) {

        if (!repo.hasPublicationData(orgId)) {
            return PublicationKpiDto.builder()
                .hasData(false)
                .parPublication(List.of())
                .build();
        }

        Optional<Map<String, Object>> globalOpt = repo.getPublicationGlobal(orgId);
        if (globalOpt.isEmpty()) {
            return PublicationKpiDto.builder()
                .hasData(false)
                .parPublication(List.of())
                .build();
        }

        Map<String, Object> g = globalOpt.get();
        int    total         = toInt(g.get("total_publications"));
        int    totalPubliees = toInt(g.get("total_publiees"));
        int    usersActifs   = toInt(g.get("total_users_actifs"));
        int    lecteurs      = toInt(g.get("nb_lecteurs_uniques"));
        int    totalLectures = toInt(g.get("total_lectures"));
        double taux          = safe(toDouble(g.get("taux_lecture_global")));

        // Une entrée par publication
        List<PublicationKpiDto.ParPublicationDto> parPublication = repo
            .getPublicationParPub(orgId, periodeJours).stream()
            .map(row -> PublicationKpiDto.ParPublicationDto.builder()
                .publicationId(toLong(row.get("publication_id")))
                .titre(str(row.get("titre")))
                .type(str(row.get("type")))
                .priorite(str(row.get("priorite")))
                .publieLe(str(row.get("publie_le")))
                .nbLecteurs(toInt(row.get("nb_lecteurs")))
                .totalUsersActifs(toInt(row.get("total_users_actifs")))
                .tauxLecture(safe(toDouble(row.get("taux_lecture"))))
                .build()
            ).collect(Collectors.toList());

        return PublicationKpiDto.builder()
            .hasData(total > 0)
            .totalPublications(total)
            .totalPubliees(totalPubliees)
            .totalUsersActifs(usersActifs)
            .nbLecteursUniques(lecteurs)
            .totalLectures(totalLectures)
            .tauxLectureGlobal(taux)
            .parPublication(parPublication)
            .build();
    }

    private double safe(double v) {
        return (Double.isNaN(v) || Double.isInfinite(v)) ? 0.0 : Math.min(v, 100.0);
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
    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); }
        catch (Exception e) { return null; }
    }
    private String str(Object v) { return v != null ? v.toString() : ""; }
}