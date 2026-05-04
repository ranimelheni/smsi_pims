package com.pims.plateform.service;

import com.pims.plateform.dto.FormationKpiDto;
import com.pims.plateform.repository.KpiQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormationKpiCalculator {

    private final KpiQueryRepository repo;

    public FormationKpiDto calculate(Long orgId, int periodeJours) {

        if (!repo.hasFormationData(orgId)) {
            return FormationKpiDto.builder()
                .hasData(false)
                .parSession(List.of())
                .build();
        }

        Optional<Map<String, Object>> globalOpt = repo.getFormationGlobal(orgId);
        if (globalOpt.isEmpty()) {
            return FormationKpiDto.builder()
                .hasData(false)
                .parSession(List.of())
                .build();
        }

        Map<String, Object> g = globalOpt.get();
        int    totalSessions      = toInt(g.get("total_sessions"));
        int    terminees          = toInt(g.get("sessions_terminees"));
        int    planifiees         = toInt(g.get("sessions_planifiees"));
        int    annulees           = toInt(g.get("sessions_annulees"));
        int    totalInscriptions  = toInt(g.get("total_inscriptions"));
        int    totalPresents      = toInt(g.get("total_presents"));
        double tauxGlobal         = safe(toDouble(g.get("taux_participation_global")));

        List<FormationKpiDto.ParSessionDto> parSession = repo
            .getFormationParSession(orgId, periodeJours).stream()
            .map(row -> FormationKpiDto.ParSessionDto.builder()
                .sessionId(toLong(row.get("session_id")))
                .titre(str(row.get("titre")))
                .type(str(row.get("type")))
                .statut(str(row.get("statut")))
                .dateSession(str(row.get("date_session")))
                .maxParticipants(row.get("max_participants") != null
                    ? toInt(row.get("max_participants")) : null)
                .nbInscrits(toInt(row.get("nb_inscrits")))
                .nbPresents(toInt(row.get("nb_presents")))
                .tauxParticipation(safe(toDouble(row.get("taux_participation"))))
                .build()
            ).collect(Collectors.toList());

        return FormationKpiDto.builder()
            .hasData(totalSessions > 0)
            .totalSessions(totalSessions)
            .sessionsTerminees(terminees)
            .sessionsPlanifiees(planifiees)
            .sessionsAnnulees(annulees)
            .totalInscriptions(totalInscriptions)
            .totalPresents(totalPresents)
            .tauxParticipationGlobal(tauxGlobal)
            .parSession(parSession)
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