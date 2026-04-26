package com.pims.plateform.service;

import com.pims.plateform.config.KpiAccessConfig;
import com.pims.plateform.dto.KpiDashboardDto;
import com.pims.plateform.dto.KpiDto;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.KpiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiService {

    private final KpiRepository    kpiRepo;
    private final KpiAccessConfig  accessConfig;

    // ── Dashboard principal ───────────────────────────────────────────────
    public KpiDashboardDto buildDashboard(User user, boolean useCache) {
        Long   orgId = user.getOrganism().getId();
        String role  = user.getRole();

        Map<String, Object> raw;
        boolean fromCache = false;

        if (useCache) {
            Map<String, Object> cache = kpiRepo.getFromCache(orgId);
            if (!cache.isEmpty()) {
                raw       = flattenCache(cache);
                fromCache = true;
            } else {
                raw = kpiRepo.getDashboard(orgId);
            }
        } else {
            raw = kpiRepo.getDashboard(orgId);
        }

        Map<String, Object> tendances = kpiRepo.getTendanceJ7(orgId);

        // Construire les KPIs filtrés par rôle
        List<KpiDto> kpis = buildKpiList(role, raw, tendances, orgId, user.getId());

        // Historique (RSSI, direction, auditeur)
        List<KpiDashboardDto.KpiHistoriquePointDto> historique = List.of();
        if (Set.of("rssi","direction","auditeur","super_admin").contains(role)) {
            historique = buildHistorique(orgId);
        }

        // Détail SOA
        KpiDashboardDto.SoaDetailDto soaDetail = null;
        if (accessConfig.canAccess(role, "conformite_soa")) {
            soaDetail = buildSoaDetail(raw);
        }

        return KpiDashboardDto.builder()
            .organismId(orgId)
            .organismNom(str(raw, "organism_nom"))
            .roleUtilisateur(role)
            .computedAt(LocalDateTime.now())
            .fromCache(fromCache)
            .kpis(kpis)
            .soaDetail(soaDetail)
            .historique(historique)
            .build();
    }

    // ── Construire la liste KPI filtrée ───────────────────────────────────
    private List<KpiDto> buildKpiList(
            String role,
            Map<String, Object> raw,
            Map<String, Object> tendances,
            Long orgId,
            Long userId) {

        List<KpiDto> result = new ArrayList<>();

        // KPI 1 — Employés évalués
        if (accessConfig.canAccess(role, "employe_evalue")) {
            double valeur = toDouble(raw.get("kpi_employe_evalue"));
            result.add(buildKpi(
                "employe_evalue", valeur,
                toInt(raw.get("kpi_employe_evalue_num")),
                toInt(raw.get("kpi_employe_evalue_den")),
                tendances, role
            ));
        }

        // KPI 2 — Participation formations
        if (accessConfig.canAccess(role, "participation_formation")) {
            double valeur;
            int num, den;
            if ("employe".equals(role)) {
                // Taux personnel
                Map<String, Object> perso = kpiRepo.getParticipationForUser(orgId, userId);
                valeur = toDouble(perso.get("taux"));
                num    = toInt(perso.get("nb_presents"));
                den    = toInt(perso.get("total_inscriptions"));
            } else {
                valeur = toDouble(raw.get("kpi_participation_formation"));
                num    = toInt(raw.get("kpi_participation_formation_num"));
                den    = toInt(raw.get("kpi_participation_formation_den"));
            }
            result.add(buildKpi("participation_formation", valeur, num, den, tendances, role));
        }

        // KPI 3 — Lecture publications
        if (accessConfig.canAccess(role, "lecture_publication")) {
            double valeur = toDouble(raw.get("kpi_lecture_publication"));
            result.add(buildKpi(
                "lecture_publication", valeur,
                toInt(raw.get("kpi_lecture_publication_num")),
                toInt(raw.get("kpi_lecture_publication_den")),
                tendances, role
            ));
        }

        // KPI 4 — Documents validés
        if (accessConfig.canAccess(role, "documents_valides")) {
            double valeur = toDouble(raw.get("kpi_documents_valides"));
            result.add(buildKpi(
                "documents_valides", valeur,
                toInt(raw.get("kpi_documents_valides_num")),
                toInt(raw.get("kpi_documents_valides_den")),
                tendances, role
            ));
        }

        // KPI 5 — Conformité SoA
        if (accessConfig.canAccess(role, "conformite_soa")) {
            double valeur = toDouble(raw.get("kpi_conformite_soa"));
            result.add(buildKpi(
                "conformite_soa", valeur,
                toInt(raw.get("kpi_conformite_soa_num")),
                toInt(raw.get("kpi_conformite_soa_den")),
                tendances, role
            ));
        }

        return result;
    }

    private KpiDto buildKpi(
            String code,
            double valeur,
            int num,
            int den,
            Map<String, Object> tendances,
            String role) {

        KpiAccessConfig.KpiMeta meta = accessConfig.getMeta(code);

        // Couleur selon seuils
        String couleur = valeur >= 80 ? "green" : valeur >= 50 ? "amber" : "red";

        // Tendance
        String tendance = "stable";
        double delta    = 0.0;
        Map<?, ?> t = (Map<?, ?>) tendances.get(code);
        if (t != null && t.get("valeur_j7") != null) {
            double vActuelle = toDouble(t.get("valeur_actuelle"));
            double vJ7       = toDouble(t.get("valeur_j7"));
            delta    = vActuelle - vJ7;
            tendance = delta > 0.5 ? "hausse" : delta < -0.5 ? "baisse" : "stable";
        }

        return KpiDto.builder()
            .code(code)
            .label(meta.label())
            .icon(meta.icon())
            .valeur(valeur)
            .numerateur(num)
            .denominateur(den)
            .unite("%")
            .tendance(tendance)
            .tendanceDelta(Math.round(delta * 100.0) / 100.0)
            .couleur(couleur)
            .visible(true)
            .build();
    }

    // ── Historique ────────────────────────────────────────────────────────
    private List<KpiDashboardDto.KpiHistoriquePointDto> buildHistorique(Long orgId) {
        return kpiRepo.getHistorique(orgId, 30).stream().map(row ->
            KpiDashboardDto.KpiHistoriquePointDto.builder()
                .date(str(row, "date"))
                .employe_evalue(toDouble(row.get("employe_evalue")))
                .participation_formation(toDouble(row.get("participation_formation")))
                .lecture_publication(toDouble(row.get("lecture_publication")))
                .documents_valides(toDouble(row.get("documents_valides")))
                .conformite_soa(toDouble(row.get("conformite_soa")))
                .build()
        ).collect(Collectors.toList());
    }

    // ── SOA Detail ────────────────────────────────────────────────────────
    private KpiDashboardDto.SoaDetailDto buildSoaDetail(Map<String, Object> raw) {
        return KpiDashboardDto.SoaDetailDto.builder()
            .totalInclus(toInt(raw.get("kpi_conformite_soa_den")))
            .nbImplemente(toInt(raw.get("kpi_conformite_soa_num")))
            .a5Inclus(toInt(raw.get("soa_a5_inclus")))
            .a6Inclus(toInt(raw.get("soa_a6_inclus")))
            .a7Inclus(toInt(raw.get("soa_a7_inclus")))
            .a8Inclus(toInt(raw.get("soa_a8_inclus")))
            .build();
    }

    // ── Snapshot journalier (01h00) ───────────────────────────────────────
    @Scheduled(cron = "0 0 1 * * *")
    public void snapshotQuotidien() {
        log.info("Snapshot KPI quotidien démarré");
        // Récupérer tous les organismes actifs via JDBC directement
        // (évite de dépendre du OrganismRepository ici)
        try {
            kpiRepo.snapshotAllOrganisms();
        } catch (Exception e) {
            log.error("Erreur snapshot KPI : ", e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Map<String, Object> flattenCache(Map<String, Object> cache) {
        Map<String, Object> flat = new HashMap<>();
        for (Map.Entry<String, Object> entry : cache.entrySet()) {
            String code = entry.getKey();
            Map<?, ?> row = (Map<?, ?>) entry.getValue();
            flat.put("kpi_" + code,          row.get("valeur"));
            flat.put("kpi_" + code + "_num", row.get("numerateur"));
            flat.put("kpi_" + code + "_den", row.get("denominateur"));
        }
        return flat;
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

    private String str(Map<String, Object> m, String k) {
        return m.get(k) != null ? m.get(k).toString() : "";
    }
}