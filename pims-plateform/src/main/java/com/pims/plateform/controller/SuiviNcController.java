package com.pims.plateform.controller;

import com.pims.plateform.entity.*;
import com.pims.plateform.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/suivi-nc")
@RequiredArgsConstructor
public class SuiviNcController {

    private final SuiviNcRepository suiviNcRepo;
    private final UserRepository    userRepo;
    private final JdbcTemplate      jdbc;

    private static final List<String> ROLES_RSSI = List.of("rssi", "super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    // ── GET sessions disponibles (celles qui ont des NC) ─────────────
    @GetMapping("/sessions")
    @Transactional
    public ResponseEntity<?> getSessions(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé au RSSI"));

        // Récupérer les sessions distinctes qui ont des NC pour cet organisme
        List<SuiviNc> all = suiviNcRepo.findByOrganismId(user.getOrganism().getId());

        // Dédupliquer par session et construire la liste
        List<Map<String, Object>> sessions = all.stream()
            .collect(Collectors.groupingBy(nc -> nc.getAuditSession().getId()))
            .entrySet().stream()
            .map(entry -> {
                AuditSession s = entry.getValue().get(0).getAuditSession();
                long totalNc   = entry.getValue().size();
                long nonTraites = entry.getValue().stream()
                    .filter(nc -> "non_traite".equals(nc.getStatutImpl())).count();
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",           s.getId());
                m.put("titre",        s.getTitre());
                m.put("norme",        s.getNorme());
                m.put("date_debut",   s.getDateDebut() != null ? s.getDateDebut().toString() : null);
                m.put("date_fin",     s.getDateFin()   != null ? s.getDateFin().toString()   : null);
                m.put("total_nc",     totalNc);
                m.put("non_traites",  nonTraites);
                return m;
            })
            .sorted(Comparator.comparing(m -> m.get("date_debut").toString(),
                Comparator.reverseOrder()))
            .collect(Collectors.toList());

        return ResponseEntity.ok(sessions);
    }

    // ── GET NC d'une session ──────────────────────────────────────────
    @GetMapping("/sessions/{sessionId}")
    @Transactional
    public ResponseEntity<?> getBySession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé au RSSI"));

        List<SuiviNc> list = suiviNcRepo.findByOrganismIdAndSessionId(
            user.getOrganism().getId(), sessionId);

        return ResponseEntity.ok(list.stream().map(this::toMap).toList());
    }

    // ── PUT évaluer une NC ───────────────────────────────────────────
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> evaluer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé au RSSI"));

        SuiviNc nc = suiviNcRepo.findById(id).orElse(null);
        if (nc == null) return ResponseEntity.notFound().build();
        if (!nc.getOrganism().getId().equals(user.getOrganism().getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        if (d.containsKey("statut_impl"))
            nc.setStatutImpl(str(d.get("statut_impl")));
        if (d.containsKey("responsable_rssi"))
            nc.setResponsableRssi(str(d.get("responsable_rssi")));
        if (d.containsKey("commentaire_rssi"))
            nc.setCommentaireRssi(str(d.get("commentaire_rssi")));
        if (d.containsKey("echeance_rssi") && d.get("echeance_rssi") != null)
            nc.setEcheanceRssi(LocalDate.parse(str(d.get("echeance_rssi"))));

        nc.setEvaluePar(user);
        nc.setEvalueAt(LocalDateTime.now());

        return ResponseEntity.ok(toMap(suiviNcRepo.save(nc)));
    }

    // ── GET KPI d'une session ─────────────────────────────────────────
    @GetMapping("/sessions/{sessionId}/kpi")
    public ResponseEntity<?> getKpiSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé au RSSI"));

        Long orgId = user.getOrganism().getId();
        return ResponseEntity.ok(buildKpi(orgId, sessionId));
    }

    // ── Calcul KPI ────────────────────────────────────────────────────
    private Map<String, Object> buildKpi(Long orgId, Long sessionId) {

       String whereClause = sessionId != null
    ? String.format(
        "WHERE organism_id = %d AND audit_session_id = %d ",
        orgId, sessionId)
    : String.format(
        "WHERE organism_id = %d ",
        orgId);

        // Global
        String sqlGlobal = """
            SELECT
                COUNT(*)                                                    AS total_nc,
                COUNT(*) FILTER (WHERE statut_audit = 'non_conforme')       AS nb_non_conforme,
                COUNT(*) FILTER (WHERE statut_audit = 'partiel')            AS nb_partiel,
                COUNT(*) FILTER (WHERE statut_impl  = 'fait')               AS nb_traites,
                COUNT(*) FILTER (WHERE statut_impl  = 'en_cours')           AS nb_en_cours,
                COUNT(*) FILTER (WHERE statut_impl  = 'non_traite')         AS nb_non_traites,
                COUNT(*) FILTER (WHERE statut_impl  = 'reporte')            AS nb_reportes,
                COUNT(*) FILTER (WHERE statut_impl  = 'accepte')            AS nb_acceptes,
                CASE WHEN COUNT(*) = 0 THEN 0 ELSE ROUND(
                    COUNT(*) FILTER (WHERE statut_impl IN ('fait','accepte'))
                    ::numeric * 100.0 / COUNT(*), 2) END                    AS taux_traitement,
                COUNT(*) FILTER (
                    WHERE echeance_rssi < CURRENT_DATE
                      AND statut_impl NOT IN ('fait','accepte'))             AS nb_en_retard
            FROM suivi_nc
            """ + whereClause;

        List<Map<String, Object>> globals = jdbc.queryForList(sqlGlobal);
        if (globals.isEmpty() || toInt(globals.get(0).get("total_nc")) == 0)
            return Map.of("has_data", false, "par_clause", List.of());

        Map<String, Object> g = globals.get(0);

        // Par clause
        String sqlClause = """
            SELECT
                SPLIT_PART(clause_code, '.', 1)                              AS clause_principale,
                COUNT(*)                                                      AS total,
                COUNT(*) FILTER (WHERE statut_impl IN ('fait','accepte'))    AS nb_traites,
                COUNT(*) FILTER (WHERE statut_impl = 'non_traite')           AS nb_non_traites,
                CASE WHEN COUNT(*) = 0 THEN 0 ELSE ROUND(
                    COUNT(*) FILTER (WHERE statut_impl IN ('fait','accepte'))
                    ::numeric * 100.0 / COUNT(*), 2) END                     AS taux_traitement
            FROM suivi_nc
            """ + whereClause + """
            GROUP BY SPLIT_PART(clause_code, '.', 1)
            ORDER BY SPLIT_PART(clause_code, '.', 1)
            """;

        List<Map<String, Object>> parClause = jdbc.queryForList(sqlClause);

        Map<String, Object> kpi = new LinkedHashMap<>();
        kpi.put("has_data",        true);
        kpi.put("total_nc",        toInt(g.get("total_nc")));
        kpi.put("nb_non_conforme", toInt(g.get("nb_non_conforme")));
        kpi.put("nb_partiel",      toInt(g.get("nb_partiel")));
        kpi.put("nb_traites",      toInt(g.get("nb_traites")));
        kpi.put("nb_en_cours",     toInt(g.get("nb_en_cours")));
        kpi.put("nb_non_traites",  toInt(g.get("nb_non_traites")));
        kpi.put("nb_reportes",     toInt(g.get("nb_reportes")));
        kpi.put("nb_acceptes",     toInt(g.get("nb_acceptes")));
        kpi.put("taux_traitement", toDouble(g.get("taux_traitement")));
        kpi.put("nb_en_retard",    toInt(g.get("nb_en_retard")));
        kpi.put("par_clause",      parClause.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("clause_principale", r.get("clause_principale"));
            m.put("total",             toInt(r.get("total")));
            m.put("nb_traites",        toInt(r.get("nb_traites")));
            m.put("nb_non_traites",    toInt(r.get("nb_non_traites")));
            m.put("taux_traitement",   toDouble(r.get("taux_traitement")));
            return m;
        }).toList());

        return kpi;
    }

    // ── Mapper ────────────────────────────────────────────────────────
    private Map<String, Object> toMap(SuiviNc nc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               nc.getId());
        m.put("clause_code",      nc.getClauseCode());
        m.put("clause_titre",     nc.getClauseTitre());
        m.put("statut_audit",     nc.getStatutAudit());
        m.put("justification",    nc.getJustification());
        m.put("action_planifiee", nc.getActionPlanifiee());
        m.put("priorite",         nc.getPriorite());
        m.put("echeance_audit",   nc.getEcheanceAudit() != null
            ? nc.getEcheanceAudit().toString() : null);
        m.put("responsable_audit",nc.getResponsableAudit());
        m.put("statut_impl",      nc.getStatutImpl());
        m.put("responsable_rssi", nc.getResponsableRssi());
        m.put("echeance_rssi",    nc.getEcheanceRssi() != null
            ? nc.getEcheanceRssi().toString() : null);
        m.put("commentaire_rssi", nc.getCommentaireRssi());
        m.put("evalue_par",       nc.getEvaluePar() != null
            ? nc.getEvaluePar().getPrenom() + " " + nc.getEvaluePar().getNom() : null);
        m.put("evalue_at",        nc.getEvalueAt() != null
            ? nc.getEvalueAt().toString() : null);
        m.put("created_at",       nc.getCreatedAt() != null
            ? nc.getCreatedAt().toString() : null);
        return m;
    }

    private String str(Object v)      { return v != null ? v.toString() : null; }
    private int toInt(Object v)       { if (v == null) return 0; if (v instanceof Number n) return n.intValue(); try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; } }
    private double toDouble(Object v) { if (v == null) return 0.0; if (v instanceof Number n) return n.doubleValue(); try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; } }
}