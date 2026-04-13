// EbiosAtelier3Controller.java
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
public class EbiosAtelier3Controller {

    private final ErmAnalyseRepository              analyseRepo;
    private final ErmPartiePrenanteRepository        ppRepo;
    private final ErmScenarioStrategiqueRepository   ssRepo;
    private final ErmSourceRisqueRepository          srRepo;
    private final ErmObjectifViseRepository          ovRepo;
    private final ErmEvenementRedouteRepository      erRepo;
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
        try { return Short.parseShort(v.toString()); }
        catch (Exception e) { return def; }
    }

    private String toJson(Object val) {
        if (val == null) return "[]";
        if (val instanceof String s) return s;
        try { return om.writeValueAsString(val); }
        catch (Exception e) { return "[]"; }
    }

    private Object fromJson(String val) {
        if (val == null || val.isBlank()) return List.of();
        try { return om.readValue(val, Object.class); }
        catch (Exception e) { return List.of(); }
    }

    // Calcul niveau risque selon matrice
    private Short calcNiveauRisque(ErmAnalyse a, short g, short v) {
        int score  = g * v;
        int seuilA = a.getSeuilAcceptable() != null ? a.getSeuilAcceptable() : 6;
        int seuilE = a.getSeuilEleve()      != null ? a.getSeuilEleve()      : 12;
        if (score <= seuilA) return 1;
        if (score <= seuilE) return 2;
        return 3;
    }

    // Calcul fiabilité cyber partie prenante
    private float calcFiabilite(ErmPartiePrenante pp) {
        int num = pp.getDependance() * pp.getPenetration();
        int den = pp.getMaturiteCyber() * pp.getConfiance();
        return den > 0 ? (float) num / den : (float) num;
    }

    // ════════════════════════════════════════════════════════════════
    // GET ATELIER 3 COMPLET
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/atelier3")
    public ResponseEntity<?> getAtelier3(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.ok(Map.of("non_initialise", true));

        List<ErmPartiePrenante>        pps = ppRepo.findByAnalyseIdOrderByLibelle(a.getId());
        List<ErmScenarioStrategique>   ss  = ssRepo.findByAnalyseIdOrderByNiveauRisqueDescGraviteDesc(a.getId());
        List<ErmSourceRisque>          srs = srRepo.findByAnalyseIdAndRetenuTrue(a.getId());
        List<ErmObjectifVise>          ovs = ovRepo.findByAnalyseIdAndRetenuTrue(a.getId());
        List<ErmEvenementRedoute>      ers = erRepo.findByAnalyseIdOrderByGraviteDesc(a.getId());

        long nbEleve = ssRepo.countByAnalyseIdAndNiveauRisque(a.getId(),(short)3);
        long nbMoyen = ssRepo.countByAnalyseIdAndNiveauRisque(a.getId(),(short)2);
        long nbFaible= ssRepo.countByAnalyseIdAndNiveauRisque(a.getId(),(short)1);

        return ResponseEntity.ok(Map.of(
            "parties_prenantes",      pps.stream().map(this::ppToMap).toList(),
            "scenarios_strategiques", ss.stream().map(this::ssToMap).toList(),
            "sources_retenues",       srs.stream().map(this::srToMap).toList(),
            "objectifs_retenus",      ovs.stream().map(this::ovToMap).toList(),
            "evenements_redoutes",    ers.stream().map(this::erToMap).toList(),
            "stats", Map.of(
                "pp_total",     pps.size(),
                "pp_retenues",  pps.stream().filter(ErmPartiePrenante::getRetenu).count(),
                "ss_total",     ss.size(),
                "ss_eleve",     nbEleve,
                "ss_moyen",     nbMoyen,
                "ss_faible",    nbFaible
            )
        ));
    }

    // ════════════════════════════════════════════════════════════════
    // PARTIES PRENANTES
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/parties-prenantes")
    public ResponseEntity<?> createPP(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmPartiePrenante pp = ErmPartiePrenante.builder()
                .analyse(a).organism(user.getOrganism())
                .libelle(str(d,"libelle"))
                .categorie(d.getOrDefault("categorie","fournisseur").toString())
                .description(str(d,"description"))
                .dependance(toShort(d.get("dependance"),(short)2))
                .penetration(toShort(d.get("penetration"),(short)2))
                .maturiteCyber(toShort(d.get("maturite_cyber"),(short)2))
                .confiance(toShort(d.get("confiance"),(short)2))
                .retenu(d.get("retenu") == null || Boolean.parseBoolean(d.get("retenu").toString()))
                .build();

        pp.setFiabiliteCyber(calcFiabilite(pp));
        return ResponseEntity.status(201).body(ppToMap(ppRepo.save(pp)));
    }

    @PutMapping("/parties-prenantes/{id}")
    public ResponseEntity<?> updatePP(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmPartiePrenante pp = ppRepo.findById(id).orElse(null);
        if (pp == null) return ResponseEntity.notFound().build();

        if (d.containsKey("libelle"))       pp.setLibelle(str(d,"libelle"));
        if (d.containsKey("categorie"))     pp.setCategorie(str(d,"categorie"));
        if (d.containsKey("description"))   pp.setDescription(str(d,"description"));
        if (d.containsKey("dependance"))    pp.setDependance(toShort(d.get("dependance"),(short)2));
        if (d.containsKey("penetration"))   pp.setPenetration(toShort(d.get("penetration"),(short)2));
        if (d.containsKey("maturite_cyber"))pp.setMaturiteCyber(toShort(d.get("maturite_cyber"),(short)2));
        if (d.containsKey("confiance"))     pp.setConfiance(toShort(d.get("confiance"),(short)2));
        if (d.containsKey("retenu"))        pp.setRetenu(Boolean.parseBoolean(d.get("retenu").toString()));

        pp.setFiabiliteCyber(calcFiabilite(pp));
        return ResponseEntity.ok(ppToMap(ppRepo.save(pp)));
    }

    @DeleteMapping("/parties-prenantes/{id}")
    public ResponseEntity<?> deletePP(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));
        ppRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // SCÉNARIOS STRATÉGIQUES
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/scenarios-strategiques")
    public ResponseEntity<?> createSS(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmSourceRisque sr = srRepo.findById(
            Long.parseLong(d.get("source_id").toString())).orElse(null);
        if (sr == null) return ResponseEntity.badRequest().body(Map.of("error","Source non trouvée"));

        ErmObjectifVise ov = ovRepo.findById(
            Long.parseLong(d.get("objectif_id").toString())).orElse(null);
        if (ov == null) return ResponseEntity.badRequest().body(Map.of("error","Objectif non trouvé"));

        ErmEvenementRedoute er = d.get("evenement_id") != null
            ? erRepo.findById(Long.parseLong(d.get("evenement_id").toString())).orElse(null) : null;

        short g = toShort(d.get("gravite"),(short)2);
        short v = toShort(d.get("vraisemblance"),(short)2);

        ErmScenarioStrategique ss = ErmScenarioStrategique.builder()
                .analyse(a).organism(user.getOrganism())
                .source(sr).objectif(ov).evenement(er)
                .libelle(str(d,"libelle"))
                .description(str(d,"description"))
                .gravite(g).vraisemblance(v)
                .niveauRisque(calcNiveauRisque(a, g, v))
                .partiesPrenantes(toJson(d.get("parties_prenantes")))
                .decisionTraitement(d.getOrDefault("decision_traitement","traiter").toString())
                .justificationDecision(str(d,"justification_decision"))
                .build();

        return ResponseEntity.status(201).body(ssToMap(ssRepo.save(ss)));
    }

    @PutMapping("/scenarios-strategiques/{id}")
    public ResponseEntity<?> updateSS(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmScenarioStrategique ss = ssRepo.findById(id).orElse(null);
        if (ss == null) return ResponseEntity.notFound().build();

        if (d.containsKey("libelle"))      ss.setLibelle(str(d,"libelle"));
        if (d.containsKey("description"))  ss.setDescription(str(d,"description"));
        if (d.containsKey("gravite"))      ss.setGravite(toShort(d.get("gravite"),(short)2));
        if (d.containsKey("vraisemblance"))ss.setVraisemblance(toShort(d.get("vraisemblance"),(short)2));
        if (d.containsKey("parties_prenantes")) ss.setPartiesPrenantes(toJson(d.get("parties_prenantes")));
        if (d.containsKey("decision_traitement"))  ss.setDecisionTraitement(str(d,"decision_traitement"));
        if (d.containsKey("justification_decision"))ss.setJustificationDecision(str(d,"justification_decision"));

        if (d.get("evenement_id") != null)
            erRepo.findById(Long.parseLong(d.get("evenement_id").toString()))
                  .ifPresent(ss::setEvenement);

        // Recalculer niveau risque
        ss.setNiveauRisque(calcNiveauRisque(ss.getAnalyse(), ss.getGravite(), ss.getVraisemblance()));

        return ResponseEntity.ok(ssToMap(ssRepo.save(ss)));
    }

    @DeleteMapping("/scenarios-strategiques/{id}")
    public ResponseEntity<?> deleteSS(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));
        ssRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════════════

    private Map<String,Object> ppToMap(ErmPartiePrenante p) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",             p.getId());
        m.put("libelle",        p.getLibelle());
        m.put("categorie",      p.getCategorie());
        m.put("description",    p.getDescription() != null ? p.getDescription() : "");
        m.put("dependance",     p.getDependance());
        m.put("penetration",    p.getPenetration());
        m.put("maturite_cyber", p.getMaturiteCyber());
        m.put("confiance",      p.getConfiance());
        m.put("fiabilite_cyber",p.getFiabiliteCyber() != null ? p.getFiabiliteCyber() : 0);
        m.put("retenu",         p.getRetenu());
        return m;
    }

    private Map<String,Object> ssToMap(ErmScenarioStrategique s) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",                     s.getId());
        m.put("libelle",                s.getLibelle());
        m.put("description",            s.getDescription() != null ? s.getDescription() : "");
        m.put("source_id",              s.getSource().getId());
        m.put("source_libelle",         s.getSource().getLibelle());
        m.put("source_categorie",       s.getSource().getCategorie());
        m.put("objectif_id",            s.getObjectif().getId());
        m.put("objectif_libelle",       s.getObjectif().getLibelle());
        m.put("evenement_id",    s.getEvenement() != null ? s.getEvenement().getId()     : null);
        m.put("evenement_libelle",s.getEvenement() != null ? s.getEvenement().getLibelle() : null);
        m.put("evenement_gravite",s.getEvenement() != null ? s.getEvenement().getGravite() : null);
        m.put("gravite",                s.getGravite());
        m.put("vraisemblance",          s.getVraisemblance());
        m.put("niveau_risque",          s.getNiveauRisque());
        m.put("niveau_risque_label",    niveauLabel(s.getNiveauRisque()));
        m.put("score_risque",           s.getGravite() * s.getVraisemblance());
        m.put("parties_prenantes",      fromJson(s.getPartiesPrenantes()));
        m.put("decision_traitement",    s.getDecisionTraitement());
        m.put("justification_decision", s.getJustificationDecision() != null ? s.getJustificationDecision() : "");
        return m;
    }

    private Map<String,Object> srToMap(ErmSourceRisque s) {
        return Map.of("id",s.getId(),"libelle",s.getLibelle(),"categorie",s.getCategorie());
    }

    private Map<String,Object> ovToMap(ErmObjectifVise o) {
        return Map.of("id",o.getId(),"libelle",o.getLibelle(),
                      "source_id",o.getSource().getId(),
                      "pertinence_retenue",o.getPertinenceRetenue());
    }

    private Map<String,Object> erToMap(ErmEvenementRedoute e) {
        return Map.of("id",e.getId(),"libelle",e.getLibelle(),"gravite",e.getGravite(),
                      "valeur_metier_id",e.getValeurMetier().getId(),
                      "valeur_metier_nom",e.getValeurMetier().getDenomination());
    }

    private String niveauLabel(Short n) {
        if (n == null) return "—";
        return switch(n) { case 1 -> "Faible"; case 2 -> "Moyen"; case 3 -> "Élevé"; default -> "—"; };
    }
}