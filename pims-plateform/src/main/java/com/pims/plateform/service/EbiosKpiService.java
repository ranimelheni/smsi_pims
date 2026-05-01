package com.pims.plateform.service;

import com.pims.plateform.dto.EbiosKpiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EbiosKpiService {

    private final JdbcTemplate jdbc;

    public EbiosKpiDto getKpi(Long analyseId, Long orgId) {

        String sql = """
            SELECT *
            FROM v_ebios_kpi_dashboard
            WHERE analyse_id = ?
              AND organism_id = ?
            """;

        List<Map<String, Object>> rows = jdbc.queryForList(sql, analyseId, orgId);
        if (rows.isEmpty()) return null;

        Map<String, Object> r = rows.get(0);

        // Comptages
        int nbValeurs   = toInt(r.get("nb_valeurs_metier"));
        int nbBiens     = toInt(r.get("nb_biens_support"));
        int nbEvts      = toInt(r.get("nb_evenements"));
        int nbSR        = toInt(r.get("nb_sources_risque"));
        int nbSS        = toInt(r.get("nb_scenarios_strat"));
        int nbSO        = toInt(r.get("nb_scenarios_op"));
        int nbMesures   = toInt(r.get("nb_mesures"));
        int nbResiduels = toInt(r.get("nb_residuels"));

        // KPIs
        double kpiCouv = toDouble(r.get("kpi_couverture"));
        double kpiRR   = toDouble(r.get("kpi_niveau_residuel"));
        double kpiMs   = toDouble(r.get("kpi_mesures"));

        // Score completion (avancement global de l'analyse)
        int completion = calcCompletion(nbValeurs, nbSR, nbSS, nbSO, nbMesures);

        // Niveau global basé sur le risque résiduel moyen
        String niveau  = getNiveauGlobal(kpiRR, kpiCouv);
        String couleur = getCouleur(kpiRR, kpiCouv);

        return EbiosKpiDto.builder()
            .analyseId(analyseId)
            .analyseTitre(str(r.get("analyse_titre")))
            .analyseStatut(str(r.get("analyse_statut")))
            // Comptages
            .nbValeursMetier(nbValeurs)
            .nbBiensSupport(nbBiens)
            .nbEvenements(nbEvts)
            .nbSourcesRisque(nbSR)
            .nbScenariosStrat(nbSS)
            .nbScenariosOp(nbSO)
            .nbMesures(nbMesures)
            .nbResiduels(nbResiduels)
            // KPI 1 couverture
            .kpiCouverture(kpiCouv)
            .couvertureNum(toInt(r.get("couverture_num")))
            .couvertureDen(toInt(r.get("couverture_den")))
            .couvertureAcceptes(toInt(r.get("couverture_acceptes")))
            // KPI 2 résiduel
            .kpiNiveauResiduel(kpiRR)
            .residuelTotal(nbResiduels)
            .residuelEleve(toInt(r.get("residuel_eleve")))
            .residuelMoyen(toInt(r.get("residuel_moyen")))
            .residuelFaible(toInt(r.get("residuel_faible")))
            // KPI 3 mesures
            .kpiMesures(kpiMs)
            .mesuresNum(toInt(r.get("mesures_num")))
            .mesuresDen(toInt(r.get("mesures_den")))
            .mesuresEnCours(toInt(r.get("mesures_en_cours")))
            .mesuresPlanifiees(toInt(r.get("mesures_planifiees")))
            // Global
            .niveauGlobal(niveau)
            .couleurGlobale(couleur)
            .scoreCompletion(completion)
            .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private int calcCompletion(int valeurs, int sources, int ss, int so, int mesures) {
        int score = 0;
        if (valeurs  > 0) score += 20;
        if (sources  > 0) score += 20;
        if (ss       > 0) score += 20;
        if (so       > 0) score += 20;
        if (mesures  > 0) score += 20;
        return score;
    }

    private String getNiveauGlobal(double kpiRR, double kpiCouv) {
        if (kpiRR == 0 && kpiCouv == 0) return "Non évalué";
        if (kpiRR >= 3.5 || kpiCouv < 30) return "Critique";
        if (kpiRR >= 2.5 || kpiCouv < 60) return "Élevé";
        if (kpiRR >= 1.5 || kpiCouv < 80) return "Moyen";
        return "Faible";
    }

    private String getCouleur(double kpiRR, double kpiCouv) {
        if (kpiRR == 0 && kpiCouv == 0) return "gray";
        if (kpiRR >= 3.5 || kpiCouv < 30) return "red";
        if (kpiRR >= 2.5 || kpiCouv < 60) return "orange";
        if (kpiRR >= 1.5 || kpiCouv < 80) return "amber";
        return "green";
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

    private String str(Object v) {
        return v != null ? v.toString() : "";
    }
}