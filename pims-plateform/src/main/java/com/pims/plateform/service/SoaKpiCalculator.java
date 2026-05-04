package com.pims.plateform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.pims.plateform.dto.SoaKpiDto;
import com.pims.plateform.repository.KpiQueryRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SoaKpiCalculator {

    private final KpiQueryRepository repo;

    public SoaKpiDto calculate(Long orgId, int periodeEvolutionJours) {

        // Vérifier l'existence de données
        if (!repo.hasSoaData(orgId)) {
            return SoaKpiDto.builder()
                .hasData(false)
                .tauxGlobal(0)
                .parAnnexe(List.of())
                .evolution(List.of())
                .build();
        }

        // KPI global
        Optional<Map<String, Object>> globalOpt = repo.getSoaGlobal(orgId);
        if (globalOpt.isEmpty()) {
            return SoaKpiDto.builder()
                .hasData(false)
                .parAnnexe(List.of())
                .evolution(List.of())
                .build();
        }

        Map<String, Object> g = globalOpt.get();
        int total     = toInt(g.get("total_controles_inclus"));
        int impl      = toInt(g.get("nb_implemente"));
        int enCours   = toInt(g.get("nb_en_cours"));
        int nonComm   = toInt(g.get("nb_non_commence"));
        double taux   = safe(toDouble(g.get("taux_global")));

        // Par annexe → radar
        List<SoaKpiDto.AnnexeDto> parAnnexe = repo.getSoaParAnnexe(orgId)
            .stream()
            .map(row -> SoaKpiDto.AnnexeDto.builder()
                .annexe(str(row.get("annexe")))
                .annexeLabel(str(row.get("annexe_label")))
                .totalInclus(toInt(row.get("total_inclus")))
                .nbImplemente(toInt(row.get("nb_implemente")))
                .tauxAnnexe(safe(toDouble(row.get("taux_annexe"))))
                .build()
            )
            .collect(Collectors.toList());

        // Évolution → line chart
        List<SoaKpiDto.EvolutionPointDto> evolution = repo
            .getSoaEvolution(orgId, periodeEvolutionJours)
            .stream()
            .map(row -> SoaKpiDto.EvolutionPointDto.builder()
                .date(str(row.get("date")))
                .tauxSoa(safe(toDouble(row.get("taux_soa"))))
                .nbImplemente(toInt(row.get("nb_implemente")))
                .totalInclus(toInt(row.get("total_inclus")))
                .build()
            )
            .collect(Collectors.toList());

        return SoaKpiDto.builder()
            .hasData(total > 0)
            .tauxGlobal(taux)
            .totalControles(total)
            .nbImplemente(impl)
            .nbEnCours(enCours)
            .nbNonCommence(nonComm)
            .parAnnexe(parAnnexe)
// Remplacez la ligne evolution dans le return final :
.evolution(fillToToday(evolution))
            .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────
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
    // Ajoutez cette méthode helper dans SoaKpiCalculator

private List<SoaKpiDto.EvolutionPointDto> fillToToday(
        List<SoaKpiDto.EvolutionPointDto> evolution) {

    if (evolution.isEmpty()) return evolution;

    SoaKpiDto.EvolutionPointDto last = evolution.get(evolution.size() - 1);
    String today = java.time.LocalDate.now().toString(); // "YYYY-MM-DD"

    // Déjà à jour → rien à faire
    if (today.equals(last.getDate())) return evolution;

    // Synthétiser un point "aujourd'hui" avec la valeur inchangée
    SoaKpiDto.EvolutionPointDto syntheticPoint = SoaKpiDto.EvolutionPointDto.builder()
        .date(today)
        .tauxSoa(last.getTauxSoa())
        .nbImplemente(last.getNbImplemente())
        .totalInclus(last.getTotalInclus())
        .build();

    List<SoaKpiDto.EvolutionPointDto> result = new ArrayList<>(evolution);
    result.add(syntheticPoint);
    return result;
}
}