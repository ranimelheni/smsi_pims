package com.pims.plateform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pims.plateform.repository.KpiQueryRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiSnapshotService {

    private final KpiQueryRepository repo;
    private final JdbcTemplate jdbc;

    /**
     * Snapshot automatique tous les jours
     */
@Scheduled(fixedRate = 60000) // toutes les 60 secondes (TEMPORAIRE)
    public void snapshotAllOrganisms() {

        List<Long> orgIds = jdbc.queryForList(
            "SELECT id FROM organisms", // ⚠️ adapte si nom différent
            Long.class
        );

        if (orgIds.isEmpty()) {
            log.warn("Aucun organisme trouvé pour snapshot KPI");
            return;
        }

        for (Long orgId : orgIds) {
            try {
                snapshotSoa(orgId);
                snapshotPublication(orgId); // optionnel (tu peux commenter si pas prêt)
            } catch (Exception e) {
                log.error("Erreur snapshot org={} : {}", orgId, e.getMessage(), e);
            }
        }
    }

    // ─────────────────────────────────────────────
    // 📊 KPI SOA
    // ─────────────────────────────────────────────
    private void snapshotSoa(Long orgId) {

        Optional<Map<String, Object>> globalOpt = repo.getSoaGlobal(orgId);

        if (globalOpt.isEmpty()) {
            log.warn("Pas de données SOA pour org={}", orgId);
            return;
        }

        Map<String, Object> g = globalOpt.get();

        double taux = toDouble(g.get("taux_global"));
        int nbImpl = toInt(g.get("nb_implemente"));
        int total = toInt(g.get("total_controles_inclus"));

        insertKpi(orgId, "conformite_soa", taux, nbImpl, total);

        log.info("Snapshot SOA enregistré org={} taux={}", orgId, taux);
    }

    // ─────────────────────────────────────────────
    // 📢 KPI Publication (optionnel mais prêt)
    // ─────────────────────────────────────────────
    private void snapshotPublication(Long orgId) {

        Optional<Map<String, Object>> globalOpt = repo.getPublicationGlobal(orgId);

        if (globalOpt.isEmpty()) {
            log.warn("Pas de données publication pour org={}", orgId);
            return;
        }

        Map<String, Object> g = globalOpt.get();

        double taux = toDouble(g.get("taux_lecture_utilisateurs"));
        int lecteurs = toInt(g.get("nb_lecteurs_uniques"));
        int destinataires = toInt(g.get("total_destinataires"));

        insertKpi(orgId, "lecture_publication", taux, lecteurs, destinataires);

        log.info("Snapshot publication enregistré org={} taux={}", orgId, taux);
    }

    // ─────────────────────────────────────────────
    // 🧠 INSERT GÉNÉRIQUE KPI
    // ─────────────────────────────────────────────
private void insertKpi(Long orgId,
                       String code,
                       double valeur,
                       int numerateur,
                       int denominateur) {

    String sql = """
        INSERT INTO kpi_historique
        (organism_id, kpi_code, valeur, numerateur, denominateur, snapshot_date)
        VALUES (?, ?, ?, ?, ?, CURRENT_DATE)
        ON CONFLICT (organism_id, kpi_code, snapshot_date)
        DO UPDATE SET
            valeur = EXCLUDED.valeur,
            numerateur = EXCLUDED.numerateur,
            denominateur = EXCLUDED.denominateur
    """;

    jdbc.update(sql, orgId, code, valeur, numerateur, denominateur);
}

    // ─────────────────────────────────────────────
    // 🔧 Helpers safe
    // ─────────────────────────────────────────────
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
}