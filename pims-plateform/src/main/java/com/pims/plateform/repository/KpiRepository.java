package com.pims.plateform.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class KpiRepository {

    private final JdbcTemplate jdbc;

    // ── Dashboard consolidé ───────────────────────────────────────────────
    public Map<String, Object> getDashboard(Long orgId) {
        String sql = """
            SELECT
                kpi_employe_evalue,
                kpi_employe_evalue_num,
                kpi_employe_evalue_den,
                kpi_participation_formation,
                kpi_participation_formation_num,
                kpi_participation_formation_den,
                kpi_lecture_publication,
                kpi_lecture_publication_num,
                kpi_lecture_publication_den,
                kpi_documents_valides,
                kpi_documents_valides_num,
                kpi_documents_valides_den,
                kpi_conformite_soa,
                kpi_conformite_soa_num,
                kpi_conformite_soa_den,
                soa_a5_inclus, soa_a6_inclus,
                soa_a7_inclus, soa_a8_inclus,
                organism_nom,
                computed_at
            FROM v_kpi_dashboard
            WHERE organism_id = ?
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    // ── Cache ─────────────────────────────────────────────────────────────
    public Map<String, Object> getFromCache(Long orgId) {
        String sql = """
            SELECT kpi_code, valeur, numerateur, denominateur, computed_at
            FROM kpi_cache
            WHERE organism_id = ?
            ORDER BY kpi_code
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId);
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            result.put(row.get("kpi_code").toString(), row);
        }
        return result;
    }

    public void refreshCache(Long orgId) {
        jdbc.update("SELECT refresh_kpi_cache(?)", orgId);
    }

    public void snapshotDaily(Long orgId) {
        jdbc.update("SELECT snapshot_kpi_daily(?)", orgId);
    }

    // ── Taux personnel pour l'employé connecté ────────────────────────────
    public Map<String, Object> getParticipationForUser(Long orgId, Long userId) {
        String sql = """
            SELECT
                COALESCE(taux, 0)              AS taux,
                COALESCE(nb_presents, 0)       AS nb_presents,
                COALESCE(total_inscriptions, 0) AS total_inscriptions
            FROM v_kpi_participation_formation_employe
            WHERE organism_id = ? AND employe_id = ?
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId, userId);
        if (rows.isEmpty())
            return Map.of("taux", 0.0, "nb_presents", 0, "total_inscriptions", 0);
        return rows.get(0);
    }

    // ── Historique 30 jours ───────────────────────────────────────────────
    public List<Map<String, Object>> getHistorique(Long orgId, int jours) {
        String sql = """
            SELECT
                snapshot_date::text AS date,
                MAX(CASE WHEN kpi_code = 'employe_evalue'          THEN valeur ELSE 0 END) AS employe_evalue,
                MAX(CASE WHEN kpi_code = 'participation_formation'  THEN valeur ELSE 0 END) AS participation_formation,
                MAX(CASE WHEN kpi_code = 'lecture_publication'      THEN valeur ELSE 0 END) AS lecture_publication,
                MAX(CASE WHEN kpi_code = 'documents_valides'        THEN valeur ELSE 0 END) AS documents_valides,
                MAX(CASE WHEN kpi_code = 'conformite_soa'           THEN valeur ELSE 0 END) AS conformite_soa
            FROM kpi_historique
            WHERE organism_id = ?
              AND snapshot_date >= CURRENT_DATE - INTERVAL '1 day' * ?
            GROUP BY snapshot_date
            ORDER BY snapshot_date ASC
            """;
        return jdbc.queryForList(sql, orgId, jours);
    }

    // ── Tendance J-7 ──────────────────────────────────────────────────────
    public Map<String, Object> getTendanceJ7(Long orgId) {
        String sql = """
            SELECT kpi_code,
                   valeur AS valeur_actuelle,
                   LAG(valeur) OVER (
                       PARTITION BY kpi_code
                       ORDER BY snapshot_date
                   ) AS valeur_j7
            FROM kpi_historique
            WHERE organism_id = ?
              AND snapshot_date >= CURRENT_DATE - 8
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId);
        Map<String, Object> result = new HashMap<>();
        for (Map<String, Object> r : rows) {
            result.put(r.get("kpi_code").toString(), r);
        }
        return result;
    }
    // Ajouter dans KpiRepository
public void snapshotAllOrganisms() {
    List<Long> ids = jdbc.queryForList(
        "SELECT id FROM organisms WHERE is_active = TRUE", Long.class);
    for (Long id : ids) {
        try {
            snapshotDaily(id);
        } catch (Exception e) {
            // Log mais continue pour les autres organismes
        }
    }
}
}