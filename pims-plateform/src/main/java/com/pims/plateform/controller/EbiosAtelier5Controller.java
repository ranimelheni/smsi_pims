package com.pims.plateform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.entity.User;
import com.pims.plateform.entity.ebios.*;
import com.pims.plateform.repository.*;
import com.pims.plateform.repository.ebios.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ebios")
@RequiredArgsConstructor
public class EbiosAtelier5Controller {

    private final ErmAnalyseRepository              analyseRepo;
    private final ErmMesureSecuriteRepository        msRepo;
    private final ErmRisqueResiduelRepository        rrRepo;
    private final ErmScenarioStrategiqueRepository   ssRepo;
    private final UserRepository                     userRepo;
    private final ObjectMapper                       om = new ObjectMapper();

    private static final List<String> ROLES_RSSI = List.of("rssi","super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    private ErmAnalyse getAnalyse(Long orgId) {
        return analyseRepo.findByOrganismId(orgId).orElse(null);
    }

    private String str(Map<String,Object> d, String k) {
        return d.get(k) != null ? d.get(k).toString() : null;
    }

    private Short toShort(Object v, short def) {
        if (v == null) return def;
        try { return Short.parseShort(v.toString()); } catch (Exception e) { return def; }
    }

    private String toJson(Object val) {
        if (val == null) return "[]";
        if (val instanceof String s) return s;
        try { return om.writeValueAsString(val); } catch (Exception e) { return "[]"; }
    }

    private Object fromJson(String val) {
        if (val == null || val.isBlank()) return List.of();
        try { return om.readValue(val, Object.class); } catch (Exception e) { return List.of(); }
    }

    private Short calcNiveauRisque(ErmAnalyse a, short g, short v) {
        int score = g * v;
        int acc   = a.getSeuilAcceptable() != null ? a.getSeuilAcceptable() : 6;
        int elv   = a.getSeuilEleve()      != null ? a.getSeuilEleve()      : 12;
        if (score <= acc) return 1;
        if (score <= elv) return 2;
        return 3;
    }

    // ════════════════════════════════════════════════════════════════
    // GET ATELIER 5 COMPLET
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/atelier5")
    public ResponseEntity<?> getAtelier5(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.ok(Map.of("non_initialise", true));

        List<ErmMesureSecurite>      ms  = msRepo.findByAnalyseIdOrderByEcheanceMoisAsc(a.getId());
        List<ErmRisqueResiduel>      rrs = rrRepo.findByAnalyseIdOrderByNiveauRisqueResiduelDesc(a.getId());
        List<ErmScenarioStrategique> sss = ssRepo.findByAnalyseIdOrderByNiveauRisqueDescGraviteDesc(a.getId());

        long planifiees  = msRepo.countByAnalyseIdAndStatut(a.getId(),"planifiee");
        long enCours     = msRepo.countByAnalyseIdAndStatut(a.getId(),"en_cours");
        long realisees   = msRepo.countByAnalyseIdAndStatut(a.getId(),"realisee");
        long abandonnees = msRepo.countByAnalyseIdAndStatut(a.getId(),"abandonnee");

        // Taux couverture : scénarios ayant au moins une mesure
        Set<Long> scenariosCouvert = new HashSet<>();
        ms.forEach(m -> {
            Object sc = fromJson(m.getScenariosCouvert());
            if (sc instanceof List<?> list) {
                list.forEach(item -> {
                    try { scenariosCouvert.add(Long.parseLong(item.toString())); }
                    catch (Exception ignored) {}
                });
            }
        });
        long tauxCouverture = sss.isEmpty() ? 0 :
            (scenariosCouvert.size() * 100L / sss.size());

        return ResponseEntity.ok(Map.of(
            "mesures_securite",      ms.stream().map(this::msToMap).toList(),
            "risques_residuels",     rrs.stream().map(this::rrToMap).toList(),
            "scenarios_strategiques",sss.stream().map(this::ssRefToMap).toList(),
            "stats", Map.of(
                "ms_total",       ms.size(),
                "planifiees",     planifiees,
                "en_cours",       enCours,
                "realisees",      realisees,
                "abandonnees",    abandonnees,
                "taux_realisation", ms.isEmpty() ? 0 : (realisees * 100 / ms.size()),
                "taux_couverture",  tauxCouverture
            )
        ));
    }

    // ════════════════════════════════════════════════════════════════
    // MESURES DE SÉCURITÉ
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/mesures-securite")
    public ResponseEntity<?> createMS(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmMesureSecurite ms = ErmMesureSecurite.builder()
                .analyse(a).organism(user.getOrganism())
                .libelle(str(d,"libelle"))
                .description(str(d,"description"))
                .typeMesure(d.getOrDefault("type_mesure","preventive").toString())
                .freinDifficulte(str(d,"frein_difficulte"))
                .coutComplexite(toShort(d.get("cout_complexite"),(short)2))
                .echeanceMois(toShort(d.get("echeance_mois"),(short)6))
                .statut(d.getOrDefault("statut","planifiee").toString())
                .responsable(str(d,"responsable"))
                .scenariosCouvert(toJson(d.get("scenarios_couverts")))
                .build();

        return ResponseEntity.status(201).body(msToMap(msRepo.save(ms)));
    }

    @PutMapping("/mesures-securite/{id}")
    public ResponseEntity<?> updateMS(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmMesureSecurite ms = msRepo.findById(id).orElse(null);
        if (ms == null) return ResponseEntity.notFound().build();

        if (d.containsKey("libelle"))          ms.setLibelle(str(d,"libelle"));
        if (d.containsKey("description"))      ms.setDescription(str(d,"description"));
        if (d.containsKey("type_mesure"))      ms.setTypeMesure(str(d,"type_mesure"));
        if (d.containsKey("frein_difficulte")) ms.setFreinDifficulte(str(d,"frein_difficulte"));
        if (d.containsKey("cout_complexite"))  ms.setCoutComplexite(toShort(d.get("cout_complexite"),(short)2));
        if (d.containsKey("echeance_mois"))    ms.setEcheanceMois(toShort(d.get("echeance_mois"),(short)6));
        if (d.containsKey("statut"))           ms.setStatut(str(d,"statut"));
        if (d.containsKey("responsable"))      ms.setResponsable(str(d,"responsable"));
        if (d.containsKey("scenarios_couverts")) ms.setScenariosCouvert(toJson(d.get("scenarios_couverts")));

        return ResponseEntity.ok(msToMap(msRepo.save(ms)));
    }

    @DeleteMapping("/mesures-securite/{id}")
    public ResponseEntity<?> deleteMS(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));
        msRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // RISQUES RÉSIDUELS
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/risques-residuels")
    public ResponseEntity<?> createRR(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmScenarioStrategique ss = ssRepo.findById(
            Long.parseLong(d.get("scenario_strategique_id").toString())).orElse(null);
        if (ss == null) return ResponseEntity.badRequest().body(Map.of("error","Scénario non trouvé"));

        // Vérifier si déjà existant pour ce scénario
        if (rrRepo.findByAnalyseIdAndScenarioStrategiqueId(a.getId(), ss.getId()).isPresent())
            return ResponseEntity.badRequest().body(Map.of("error","Risque résiduel déjà défini pour ce scénario. Utilisez PUT pour le modifier."));

        short g = toShort(d.get("gravite_residuelle"),(short)1);
        short v = toShort(d.get("vraisemblance_residuelle"),(short)1);

        ErmRisqueResiduel rr = ErmRisqueResiduel.builder()
                .analyse(a).organism(user.getOrganism())
                .scenarioStrategique(ss)
                .graviteResiduelle(g).vraisemblanceResiduelle(v)
                .niveauRisqueResiduel(calcNiveauRisque(a, g, v))
                .decision(d.getOrDefault("decision","traiter").toString())
                .justification(str(d,"justification"))
                .build();

        return ResponseEntity.status(201).body(rrToMap(rrRepo.save(rr)));
    }

    @PutMapping("/risques-residuels/{id}")
    public ResponseEntity<?> updateRR(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmRisqueResiduel rr = rrRepo.findById(id).orElse(null);
        if (rr == null) return ResponseEntity.notFound().build();

        if (d.containsKey("gravite_residuelle"))
            rr.setGraviteResiduelle(toShort(d.get("gravite_residuelle"),(short)1));
        if (d.containsKey("vraisemblance_residuelle"))
            rr.setVraisemblanceResiduelle(toShort(d.get("vraisemblance_residuelle"),(short)1));
        if (d.containsKey("decision"))      rr.setDecision(str(d,"decision"));
        if (d.containsKey("justification")) rr.setJustification(str(d,"justification"));

        rr.setNiveauRisqueResiduel(calcNiveauRisque(
            rr.getAnalyse(), rr.getGraviteResiduelle(), rr.getVraisemblanceResiduelle()));

        return ResponseEntity.ok(rrToMap(rrRepo.save(rr)));
    }

    @DeleteMapping("/risques-residuels/{id}")
    public ResponseEntity<?> deleteRR(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));
        rrRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════════════

    private Map<String,Object> msToMap(ErmMesureSecurite m) {
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("id",               m.getId());
        r.put("libelle",          m.getLibelle());
        r.put("description",      m.getDescription() != null ? m.getDescription() : "");
        r.put("type_mesure",      m.getTypeMesure());
        r.put("frein_difficulte", m.getFreinDifficulte() != null ? m.getFreinDifficulte() : "");
        r.put("cout_complexite",  m.getCoutComplexite());
        r.put("echeance_mois",    m.getEcheanceMois());
        r.put("statut",           m.getStatut());
        r.put("responsable",      m.getResponsable() != null ? m.getResponsable() : "");
        r.put("scenarios_couverts", fromJson(m.getScenariosCouvert()));
        r.put("created_at", m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
        r.put("updated_at", m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
        return r;
    }

    private Map<String,Object> rrToMap(ErmRisqueResiduel r) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",                      r.getId());
        m.put("scenario_strategique_id", r.getScenarioStrategique().getId());
        m.put("scenario_libelle",        r.getScenarioStrategique().getLibelle());
        m.put("scenario_niveau_initial", r.getScenarioStrategique().getNiveauRisque());
        m.put("gravite_residuelle",      r.getGraviteResiduelle());
        m.put("vraisemblance_residuelle",r.getVraisemblanceResiduelle());
        m.put("niveau_risque_residuel",  r.getNiveauRisqueResiduel());
        m.put("niveau_label",            niveauLabel(r.getNiveauRisqueResiduel()));
        m.put("score_residuel",          r.getGraviteResiduelle() * r.getVraisemblanceResiduelle());
        m.put("decision",                r.getDecision());
        m.put("justification",           r.getJustification() != null ? r.getJustification() : "");
        return m;
    }

    private Map<String,Object> ssRefToMap(ErmScenarioStrategique s) {
        return Map.of(
            "id",           s.getId(),
            "libelle",      s.getLibelle(),
            "niveau_risque",s.getNiveauRisque() != null ? s.getNiveauRisque() : 0,
            "gravite",      s.getGravite(),
            "vraisemblance",s.getVraisemblance(),
            "score",        s.getGravite() * s.getVraisemblance()
        );
    }

    private String niveauLabel(Short n) {
        if (n == null) return "—";
        return switch(n) { case 1 -> "Faible"; case 2 -> "Moyen"; case 3 -> "Élevé"; default -> "—"; };
    }
}