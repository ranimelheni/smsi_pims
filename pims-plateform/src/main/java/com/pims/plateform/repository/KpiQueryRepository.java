package com.pims.plateform.repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class KpiQueryRepository {

    private final JdbcTemplate jdbc;

    // ═══════════════════════════════════════════════════════════════
    // SOA
    // ═══════════════════════════════════════════════════════════════

    public Optional<Map<String, Object>> getSoaGlobal(Long orgId) {
        try {
            String sql = """
                SELECT total_controles_inclus, nb_implemente,
                       nb_en_cours, nb_non_commence, taux_global
                FROM v_kpi_soa_global
                WHERE organism_id = ?
                """;
            List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            log.warn("SOA global KPI indisponible org={} : {}", orgId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> getSoaParAnnexe(Long orgId) {
        try {
            String sql = """
                SELECT annexe, annexe_label,
                       total_inclus, nb_implemente, taux_annexe
                FROM v_kpi_soa_par_annexe
                WHERE organism_id = ?
                ORDER BY annexe
                """;
            return jdbc.queryForList(sql, orgId);
        } catch (Exception e) {
            log.warn("SOA par annexe KPI indisponible org={} : {}", orgId, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getSoaEvolution(Long orgId, int mois) {
        try {
            String sql = """
                SELECT date_snapshot::text AS date,
                       taux_soa, nb_implemente, total_inclus
                FROM v_kpi_soa_evolution
                WHERE organism_id = ?
                  AND date_snapshot >= CURRENT_DATE - (? * INTERVAL '1 month')
                ORDER BY date_snapshot
                """;
            return jdbc.queryForList(sql, orgId, mois);
        } catch (Exception e) {
            log.warn("SOA évolution KPI indisponible org={} : {}", orgId, e.getMessage());
            return List.of();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PUBLICATIONS
    // ═══════════════════════════════════════════════════════════════

    public Optional<Map<String, Object>> getPublicationGlobal(Long orgId) {
        try {
            String sql = """
                SELECT total_publications, total_destinataires,
                       nb_lecteurs_uniques, total_lectures,
                       taux_lecture_utilisateurs
                FROM v_kpi_publication_global
                WHERE organism_id = ?
                """;
            List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId);
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        } catch (Exception e) {
            log.warn("Publication global KPI indisponible org={} : {}", orgId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> getPublicationParType(Long orgId) {
        try {
            String sql = """
                SELECT type_publication, nb_publications,
                       nb_lectures, taux_lecture
                FROM v_kpi_publication_par_type
                WHERE organism_id = ?
                ORDER BY nb_publications DESC
                """;
            return jdbc.queryForList(sql, orgId);
        } catch (Exception e) {
            log.warn("Publication par type KPI indisponible org={} : {}", orgId, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getPublicationParPriorite(Long orgId) {
        try {
            String sql = """
                SELECT priorite, nb_publications,
                       nb_lecteurs, taux_lecture
                FROM v_kpi_publication_par_priorite
                WHERE organism_id = ?
                ORDER BY
                    CASE priorite
                        WHEN 'urgente' THEN 1
                        WHEN 'haute'   THEN 2
                        WHEN 'normale' THEN 3
                        WHEN 'basse'   THEN 4
                        ELSE 5
                    END
                """;
            return jdbc.queryForList(sql, orgId);
        } catch (Exception e) {
            log.warn("Publication par priorité KPI indisponible org={} : {}", orgId, e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> getPublicationEvolutionMensuelle(Long orgId, int mois) {
        try {
            String sql = """
                SELECT TO_CHAR(mois, 'YYYY-MM') AS mois,
                       nb_publications, nb_lectures, nb_lecteurs_uniques
                FROM v_kpi_publication_evolution_mensuelle
                WHERE organism_id = ?
                  AND mois >= DATE_TRUNC('month', CURRENT_DATE)
                      - (? * INTERVAL '1 month')
                ORDER BY mois
                """;
            return jdbc.queryForList(sql, orgId, mois);
        } catch (Exception e) {
            log.warn("Publication évolution KPI indisponible org={} : {}", orgId, e.getMessage());
            return List.of();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VÉRIFICATION DONNÉES
    // ═══════════════════════════════════════════════════════════════

public boolean hasSoaData(Long orgId) {
    try {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM v_kpi_soa_global WHERE organism_id = ?",
            Integer.class, orgId);
        return count != null && count > 0;
    } catch (Exception e) {
        return false;
    }
}

public boolean hasPublicationData(Long orgId) {
    try {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM v_kpi_publication_global WHERE organism_id = ?",
            Integer.class, orgId);
        return count != null && count > 0;
    } catch (Exception e) {
        return false;
    }
}
}