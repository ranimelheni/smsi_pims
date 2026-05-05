package com.pims.plateform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.dto.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditSessionRepository    sessionRepo;
    private final AuditEvaluationRepository evalRepo;
    private final UserRepository            userRepo;
    private final JdbcTemplate              jdbc;
    private final ObjectMapper              om;

    private static final List<String> ROLES_AUDIT =
        List.of("auditeur", "rssi", "super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    // ════════════════════════════════════════════════════════════
    // SESSIONS
    // ════════════════════════════════════════════════════════════

    @GetMapping("/sessions")
    @Transactional
    public ResponseEntity<?> getSessions(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_AUDIT.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        List<AuditSession> sessions =
            sessionRepo.findByOrganismId(user.getOrganism().getId());

        return ResponseEntity.ok(sessions.stream().map(this::sessionToMap).toList());
    }

    @PostMapping("/sessions")
    @Transactional
    public ResponseEntity<?> createSession(
            @RequestBody Map<String, Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!"auditeur".equals(user.getRole()) && !"super_admin".equals(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à l'auditeur"));

        AuditSession session = AuditSession.builder()
            .organism(user.getOrganism())
            .auditeur(user)
            .titre(d.getOrDefault("titre", "Analyse des écarts").toString())
            .norme(d.getOrDefault("norme", "iso27001").toString())
            .dateDebut(LocalDate.now())
            .build();

        session = sessionRepo.save(session);

        // Initialiser les évaluations depuis le référentiel
        initEvaluations(session);

        return ResponseEntity.status(201).body(sessionToMap(session));
    }

    private void initEvaluations(AuditSession session) {
        String sql = """
            SELECT code, titre FROM audit_clause_referentiel
            WHERE norme = ? ORDER BY ordre
            """;
        List<Map<String, Object>> clauses = jdbc.queryForList(sql, session.getNorme());

        List<AuditEvaluation> evals = clauses.stream().map(c ->
            AuditEvaluation.builder()
                .session(session)
                .clauseCode(str(c.get("code")))
                .clauseTitre(str(c.get("titre")))
                .statut("non_evalue")
                .build()
        ).collect(Collectors.toList());

        evalRepo.saveAll(evals);
    }

    // ════════════════════════════════════════════════════════════
    // CONTEXTE RSSI — lecture seule pour l'auditeur
    // ════════════════════════════════════════════════════════════

    @GetMapping("/contexte")
    @Transactional
    public ResponseEntity<?> getContexte(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_AUDIT.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        Long orgId = user.getOrganism().getId();

        // Clause 4 — contexte
        Map<String, Object> clause4 = queryFirst(
            "SELECT perimetre_smsi, enjeux_externes, enjeux_internes, parties_interessees FROM clause4 WHERE organism_id = ?",
            orgId
        );

        // Méthodologie risque
        Map<String, Object> methodo = queryFirst(
            """
            SELECT methode, statut, echelle_probabilite, echelle_impact,
                   seuil_acceptable, seuil_eleve, labels_probabilite, labels_impact
            FROM methodologie_risque WHERE organism_id = ?
            """, orgId
        );

        // SOA
        Map<String, Object> soa = queryFirst(
            """
            SELECT s.statut, s.version,
                   COUNT(sc.id) AS nb_controles,
                   COUNT(sc.id) FILTER (WHERE sc.statut_impl = 'implemente') AS nb_implemente,
                   CASE WHEN COUNT(sc.id) = 0 THEN 0
                        ELSE ROUND(COUNT(sc.id) FILTER (WHERE sc.statut_impl = 'implemente')
                             * 100.0 / COUNT(sc.id), 2)
                   END AS taux
            FROM soa s
            LEFT JOIN soa_controles sc ON sc.soa_id = s.id AND sc.inclus = TRUE
            WHERE s.organism_id = ?
            GROUP BY s.statut, s.version
            """, orgId
        );

        AuditContexteDto dto = AuditContexteDto.builder()
            .organismNom(user.getOrganism().getNom())
            .perimetreSmsi(clause4 != null ? str(clause4.get("perimetre_smsi")) : null)
            .enjeuxExternes(clause4 != null ? parseJson(clause4.get("enjeux_externes")) : null)
            .enjeuxInternes(clause4 != null ? parseJson(clause4.get("enjeux_internes")) : null)
            .partiesInteressees(clause4 != null ? parseJson(clause4.get("parties_interessees")) : null)
            .methodeRisque(methodo != null ? str(methodo.get("methode")) : null)
            .methodeStatut(methodo != null ? str(methodo.get("statut")) : null)
            .echelleProbabilite(methodo != null ? toInt(methodo.get("echelle_probabilite")) : null)
            .echelleImpact(methodo != null ? toInt(methodo.get("echelle_impact")) : null)
            .seuilAcceptable(methodo != null ? toInt(methodo.get("seuil_acceptable")) : null)
            .seuilEleve(methodo != null ? toInt(methodo.get("seuil_eleve")) : null)
            .labelsProbabilite(methodo != null ? parseJson(methodo.get("labels_probabilite")) : null)
            .labelsImpact(methodo != null ? parseJson(methodo.get("labels_impact")) : null)
            .soaStatut(soa != null ? str(soa.get("statut")) : null)
            .soaVersion(soa != null ? str(soa.get("version")) : null)
            .soaNbControles(soa != null ? toInt(soa.get("nb_controles")) : 0)
            .soaNbImplemente(soa != null ? toInt(soa.get("nb_implemente")) : 0)
            .soaTaux(soa != null ? toDouble(soa.get("taux")) : 0.0)
            .build();

        return ResponseEntity.ok(dto);
    }

    // ════════════════════════════════════════════════════════════
    // ÉVALUATIONS
    // ════════════════════════════════════════════════════════════

    @GetMapping("/sessions/{sessionId}/evaluations")
    @Transactional
    public ResponseEntity<?> getEvaluations(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        AuditSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        if (!session.getOrganism().getId().equals(user.getOrganism().getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        // Charger évaluations + métadonnées du référentiel
        List<Map<String, Object>> refMap = jdbc.queryForList(
            "SELECT code, titre, description, parent_code FROM audit_clause_referentiel WHERE norme = ? ORDER BY ordre",
            session.getNorme()
        );

        List<AuditEvaluation> evals = evalRepo.findBySessionId(sessionId);
        Map<String, AuditEvaluation> evalByCode = evals.stream()
            .collect(Collectors.toMap(AuditEvaluation::getClauseCode, e -> e));

        List<AuditEvaluationDto> result = refMap.stream().map(r -> {
            String code = str(r.get("code"));
            AuditEvaluation ev = evalByCode.get(code);
            return AuditEvaluationDto.builder()
                .id(ev != null ? ev.getId() : null)
                .clauseCode(code)
                .clauseTitre(str(r.get("titre")))
                .clauseDesc(str(r.get("description")))
                .parentCode(str(r.get("parent_code")))
                .statut(ev != null ? ev.getStatut() : "non_evalue")
                .justification(ev != null ? ev.getJustification() : null)
                .actionPlanifiee(ev != null ? ev.getActionPlanifiee() : null)
                .priorite(ev != null ? ev.getPriorite() : "normale")
                .echeance(ev != null && ev.getEcheance() != null ? ev.getEcheance().toString() : null)
                .responsable(ev != null ? ev.getResponsable() : null)
                .build();
        }).toList();

        return ResponseEntity.ok(result);
    }

    @PutMapping("/sessions/{sessionId}/evaluations/{clauseCode}")
    @Transactional
    public ResponseEntity<?> updateEvaluation(
            @PathVariable Long sessionId,
            @PathVariable String clauseCode,
            @RequestBody Map<String, Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!"auditeur".equals(user.getRole()) && !"super_admin".equals(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à l'auditeur"));

        AuditSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        if (!session.getOrganism().getId().equals(user.getOrganism().getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        AuditEvaluation eval = evalRepo
            .findBySessionIdAndClauseCode(sessionId, clauseCode)
            .orElseGet(() -> {
                AuditEvaluation e = new AuditEvaluation();
                e.setSession(session);
                e.setClauseCode(clauseCode);
                return e;
            });

        if (d.containsKey("statut"))          eval.setStatut(str(d.get("statut")));
        if (d.containsKey("justification"))   eval.setJustification(str(d.get("justification")));
        if (d.containsKey("action_planifiee"))eval.setActionPlanifiee(str(d.get("action_planifiee")));
        if (d.containsKey("priorite"))        eval.setPriorite(str(d.get("priorite")));
        if (d.containsKey("responsable"))     eval.setResponsable(str(d.get("responsable")));
        if (d.containsKey("echeance") && d.get("echeance") != null)
            eval.setEcheance(LocalDate.parse(str(d.get("echeance"))));

        evalRepo.save(eval);
        return ResponseEntity.ok(Map.of("message", "Évaluation mise à jour"));
    }

    // ════════════════════════════════════════════════════════════
    // KPI AUDIT
    // ════════════════════════════════════════════════════════════

    @GetMapping("/sessions/{sessionId}/kpi")
    @Transactional
    public ResponseEntity<?> getKpi(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        AuditSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        if (!session.getOrganism().getId().equals(user.getOrganism().getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        // KPI global depuis vue
        Map<String, Object> global = queryFirst(
            "SELECT * FROM v_audit_kpi WHERE session_id = ?", sessionId
        );

        // KPI par clause
        List<Map<String, Object>> parClause = jdbc.queryForList(
            "SELECT * FROM v_audit_kpi_par_clause WHERE session_id = ? ORDER BY clause_principale",
            sessionId
        );

        if (global == null) {
            return ResponseEntity.ok(AuditKpiDto.builder()
                .sessionId(sessionId)
                .parClause(List.of())
                .build());
        }

        List<AuditKpiDto.ClauseKpiDto> clauseKpis = parClause.stream().map(r ->
            AuditKpiDto.ClauseKpiDto.builder()
                .clausePrincipale(str(r.get("clause_principale")))
                .total(toInt(r.get("total")))
                .nbConforme(toInt(r.get("nb_conforme")))
                .nbNonConforme(toInt(r.get("nb_non_conforme")))
                .nbPartiel(toInt(r.get("nb_partiel")))
                .tauxClause(safeDouble(toDouble(r.get("taux_clause"))))
                .build()
        ).toList();

        AuditKpiDto kpi = AuditKpiDto.builder()
            .sessionId(sessionId)
            .totalClausesEvaluees(toInt(global.get("total_clauses_evaluees")))
            .nbConforme(toInt(global.get("nb_conforme")))
            .nbNonConforme(toInt(global.get("nb_non_conforme")))
            .nbPartiel(toInt(global.get("nb_partiel")))
            .nbPlanifie(toInt(global.get("nb_planifie")))
            .nbEnCours(toInt(global.get("nb_en_cours")))
            .nbNonApplicable(toInt(global.get("nb_non_applicable")))
            .nbNonEvalue(toInt(global.get("nb_non_evalue")))
            .tauxConformiteGlobale(safeDouble(toDouble(global.get("taux_conformite_globale"))))
            .tauxConformitePartielle(safeDouble(toDouble(global.get("taux_conformite_partielle"))))
            .parClause(clauseKpis)
            .build();

        return ResponseEntity.ok(kpi);
    }

    @PatchMapping("/sessions/{sessionId}/finaliser")
    @Transactional
    public ResponseEntity<?> finaliser(
            @PathVariable Long sessionId,
            @RequestBody(required = false) Map<String, Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        AuditSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();
        if (!session.getOrganism().getId().equals(user.getOrganism().getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        session.setStatut("finalise");
        session.setDateFin(LocalDate.now());
        if (d != null && d.get("commentaire_global") != null)
            session.setCommentaireGlobal(str(d.get("commentaire_global")));

        sessionRepo.save(session);
        return ResponseEntity.ok(Map.of("message", "Session finalisée"));
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private Map<String, Object> sessionToMap(AuditSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         s.getId());
        m.put("titre",      s.getTitre());
        m.put("norme",      s.getNorme());
        m.put("statut",     s.getStatut());
        m.put("version",    s.getVersion());
        m.put("date_debut", s.getDateDebut() != null ? s.getDateDebut().toString() : null);
        m.put("date_fin",   s.getDateFin()   != null ? s.getDateFin().toString()   : null);
        m.put("auditeur",   s.getAuditeur().getPrenom() + " " + s.getAuditeur().getNom());
        m.put("created_at", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> queryFirst(String sql, Object... args) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("Query error: {}", e.getMessage());
            return null;
        }
    }

    private Object parseJson(Object v) {
        if (v == null) return null;
        try { return om.readValue(v.toString(), Object.class); }
        catch (Exception e) { return v.toString(); }
    }

    private String    str(Object v)    { return v != null ? v.toString() : null; }
    private int    toInt(Object v)     { if (v == null) return 0; if (v instanceof Number n) return n.intValue(); try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; } }
    private double toDouble(Object v)  { if (v == null) return 0.0; if (v instanceof Number n) return n.doubleValue(); try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; } }
    private double safeDouble(double v){ return (Double.isNaN(v) || Double.isInfinite(v)) ? 0.0 : v; }
}