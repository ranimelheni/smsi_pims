package com.pims.plateform.service;

import com.pims.plateform.repository.KpiQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiSnapshotService {

    private final KpiQueryRepository repo;
    private final JdbcTemplate jdbc;

    // ⛔ PAS chaque minute → toutes les 10 minutes (démo)
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void snapshotAllOrganisms() {

        List<Long> orgIds = jdbc.queryForList(
                "SELECT id FROM organisms",
                Long.class
        );

        for (Long orgId : orgIds) {
            try {
                snapshotSoa(orgId);
                snapshotFormation(orgId);

            } catch (Exception e) {
                log.error("Snapshot error org={} : {}", orgId, e.getMessage(), e);
            }
        }
    }

    // ═══════════════════════════════════════
    // SOA SNAPSHOT
    // ═══════════════════════════════════════
    private void snapshotSoa(Long orgId) {

        Optional<Map<String, Object>> globalOpt = repo.getSoaGlobal(orgId);
        if (globalOpt.isEmpty()) return;

        Map<String, Object> g = globalOpt.get();

        double taux = toDouble(g.get("taux_global"));
        int nbImpl = toInt(g.get("nb_implemente"));
        int total = toInt(g.get("total_controles_inclus"));

        // ⛔ éviter doublon
        if (!hasChanged(orgId, "conformite_soa", taux)) {
            return;
        }

        insertKpi(orgId, "conformite_soa", taux, nbImpl, total);

        log.info("SOA snapshot OK org={} taux={}", orgId, taux);
    }

 private void snapshotFormation(Long orgId) {
    Optional<Map<String, Object>> globalOpt = repo.getFormationGlobal(orgId);
    if (globalOpt.isEmpty()) return;

    Map<String, Object> g = globalOpt.get();
    double taux      = toDouble(g.get("taux_participation_global"));
    int    presents  = toInt(g.get("total_presents"));
    int    inscrits  = toInt(g.get("total_inscriptions"));

    if (!hasChanged(orgId, "participation_formation", taux)) return;

    insertKpi(orgId, "participation_formation", taux, presents, inscrits);
    log.info("Formation snapshot OK org={} taux={}", orgId, taux);
}

    // ═══════════════════════════════════════
    // INSERT KPI
    // ═══════════════════════════════════════
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

    // ═══════════════════════════════════════
    // CHECK CHANGEMENT
    // ═══════════════════════════════════════
    private boolean hasChanged(Long orgId, String code, double newValue) {

        String sql = """
            SELECT valeur
            FROM kpi_historique
            WHERE organism_id = ? AND kpi_code = ?
            ORDER BY snapshot_date DESC
            LIMIT 1
        """;

        List<Double> last = jdbc.query(sql,
                (rs, rowNum) -> rs.getDouble("valeur"),
                orgId, code
        );

        if (last.isEmpty()) return true;

        return Math.abs(last.get(0) - newValue) > 0.0001;
    }

    // ═══════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════
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