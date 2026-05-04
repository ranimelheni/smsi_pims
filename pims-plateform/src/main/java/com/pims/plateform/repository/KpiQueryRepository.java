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

    public List<Map<String, Object>> getSoaEvolution(Long orgId, int jours) {
    try {
        String sql = """
            SELECT date_snapshot::text AS date,
                   taux_soa, nb_implemente, total_inclus
            FROM v_kpi_soa_evolution
            WHERE organism_id = ?
              AND date_snapshot >= CURRENT_DATE - (? * INTERVAL '1 day')
            ORDER BY date_snapshot
        """;
        return jdbc.queryForList(sql, orgId, jours);
    } catch (Exception e) {
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
// ═══════════════════════════════════════════════════════════════
    // PUBLICATION — nouveau
    // ═══════════════════════════════════════════════════════════════

    public Optional<Map<String, Object>> getPublicationGlobal(Long orgId) {
    try {
        String sql = """
            SELECT total_publications, total_publiees,
                   total_users_actifs, nb_lecteurs_uniques,
                   total_lectures, taux_lecture_global
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

public List<Map<String, Object>> getPublicationParPub(Long orgId, int jours) {
    try {
        String sql = """
            SELECT publication_id, titre, type, priorite,
                   publie_le::text AS publie_le,
                   nb_lecteurs, total_users_actifs, taux_lecture
            FROM v_kpi_publication_par_pub
            WHERE organism_id = ?
              AND publie_le >= CURRENT_DATE - (? * INTERVAL '1 day')
            ORDER BY publie_le ASC
            """;
        return jdbc.queryForList(sql, orgId, jours);
    } catch (Exception e) {
        log.warn("Publication par pub KPI indisponible org={} : {}", orgId, e.getMessage());
        return List.of();
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
// ═══════════════════════════════════════════════════════════════
// FORMATION
// ═══════════════════════════════════════════════════════════════

public Optional<Map<String, Object>> getFormationGlobal(Long orgId) {
    try {
        String sql = """
            SELECT total_sessions, sessions_terminees, sessions_planifiees,
                   sessions_annulees, total_inscriptions, total_presents,
                   taux_participation_global
            FROM v_kpi_formation_global
            WHERE organism_id = ?
            """;
        List<Map<String, Object>> rows = jdbc.queryForList(sql, orgId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    } catch (Exception e) {
        log.warn("Formation global KPI indisponible org={} : {}", orgId, e.getMessage());
        return Optional.empty();
    }
}

public List<Map<String, Object>> getFormationParSession(Long orgId, int jours) {
    try {
        String sql = """
            SELECT session_id, titre, type, statut,
                   date_session::text AS date_session,
                   max_participants, nb_inscrits,
                   nb_presents, taux_participation
            FROM v_kpi_formation_par_session
            WHERE organism_id = ?
              AND date_session >= CURRENT_DATE - (? * INTERVAL '1 day')
            ORDER BY date_session ASC
            """;
        return jdbc.queryForList(sql, orgId, jours);
    } catch (Exception e) {
        log.warn("Formation par session KPI indisponible org={} : {}", orgId, e.getMessage());
        return List.of();
    }
}

public boolean hasFormationData(Long orgId) {
    try {
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM v_kpi_formation_global WHERE organism_id = ?",
            Integer.class, orgId);
        return count != null && count > 0;
    } catch (Exception e) {
        return false;
    }
}
}