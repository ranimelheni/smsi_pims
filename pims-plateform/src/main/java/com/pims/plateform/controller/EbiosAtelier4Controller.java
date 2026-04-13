package com.pims.plateform.controller;

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
public class EbiosAtelier4Controller {

    private final ErmAnalyseRepository              analyseRepo;
    private final ErmScenarioOperationnelRepository  soRepo;
    private final ErmActionElementaireRepository     aeRepo;
    private final ErmScenarioStrategiqueRepository   ssRepo;
    private final ErmBienSupportRepository           bsRepo;
    private final UserRepository                     userRepo;

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

    private Short calcNiveauRisque(ErmAnalyse a, short g, short v) {
        int score = g * v;
        int acc   = a.getSeuilAcceptable() != null ? a.getSeuilAcceptable() : 6;
        int elv   = a.getSeuilEleve()      != null ? a.getSeuilEleve()      : 12;
        if (score <= acc) return 1;
        if (score <= elv) return 2;
        return 3;
    }

    // Table EBIOS RM : vraisemblance action = f(probabilité × difficulté)
    private Short calcVraisemblanceAction(short prob, short diff) {
        int score = prob * diff;
        if (score <= 1)  return 1;
        if (score <= 4)  return 2;
        if (score <= 9)  return 3;
        return 4;
    }

    // ════════════════════════════════════════════════════════════════
    // GET ATELIER 4 COMPLET
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/atelier4")
    public ResponseEntity<?> getAtelier4(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.ok(Map.of("non_initialise", true));

        List<ErmScenarioOperationnel> sos = soRepo
                .findByAnalyseIdOrderByNiveauRisqueDescGraviteDesc(a.getId());

        List<ErmScenarioStrategique> sss = ssRepo
                .findByAnalyseIdOrderByNiveauRisqueDescGraviteDesc(a.getId());

        List<ErmBienSupport> bs = bsRepo.findByAnalyseIdOrderByDenomination(a.getId());

        // Actions groupées par SO
        Map<Long, List<Map<String,Object>>> actionsParSO = new LinkedHashMap<>();
        sos.forEach(so -> actionsParSO.put(so.getId(),
            aeRepo.findByScenarioOpIdOrderByNumero(so.getId())
                  .stream().map(this::aeToMap).toList()));

        long nbEleve  = soRepo.countByAnalyseIdAndNiveauRisque(a.getId(),(short)3);
        long nbMoyen  = soRepo.countByAnalyseIdAndNiveauRisque(a.getId(),(short)2);
        long nbFaible = soRepo.countByAnalyseIdAndNiveauRisque(a.getId(),(short)1);
        long nbActions = aeRepo.countByAnalyseId(a.getId());

        return ResponseEntity.ok(Map.of(
            "scenarios_operationnels", sos.stream().map(this::soToMap).toList(),
            "actions_par_so",          actionsParSO,
            "scenarios_strategiques",  sss.stream().map(this::ssRefToMap).toList(),
            "biens_support",           bs.stream().map(this::bsRefToMap).toList(),
            "stats", Map.of(
                "so_total",  sos.size(),
                "so_eleve",  nbEleve,
                "so_moyen",  nbMoyen,
                "so_faible", nbFaible,
                "nb_actions",nbActions
            )
        ));
    }

    // ════════════════════════════════════════════════════════════════
    // SCÉNARIOS OPÉRATIONNELS
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/scenarios-operationnels")
    public ResponseEntity<?> createSO(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmScenarioStrategique ss = ssRepo.findById(
            Long.parseLong(d.get("scenario_strategique_id").toString())).orElse(null);
        if (ss == null) return ResponseEntity.badRequest().body(Map.of("error","Scénario stratégique non trouvé"));

        ErmBienSupport bs = d.get("bien_support_id") != null
            ? bsRepo.findById(Long.parseLong(d.get("bien_support_id").toString())).orElse(null) : null;

        short g = toShort(d.get("gravite"),(short)2);
        short v = toShort(d.get("vraisemblance"),(short)2);

        ErmScenarioOperationnel so = ErmScenarioOperationnel.builder()
                .analyse(a).organism(user.getOrganism())
                .scenarioStrategique(ss).bienSupport(bs)
                .libelle(str(d,"libelle"))
                .description(str(d,"description"))
                .canalExfiltration(str(d,"canal_exfiltration"))
                .gravite(g).vraisemblance(v)
                .niveauRisque(calcNiveauRisque(a, g, v))
                .build();

        return ResponseEntity.status(201).body(soToMap(soRepo.save(so)));
    }

    @PutMapping("/scenarios-operationnels/{id}")
    public ResponseEntity<?> updateSO(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmScenarioOperationnel so = soRepo.findById(id).orElse(null);
        if (so == null) return ResponseEntity.notFound().build();

        if (d.containsKey("libelle"))           so.setLibelle(str(d,"libelle"));
        if (d.containsKey("description"))       so.setDescription(str(d,"description"));
        if (d.containsKey("canal_exfiltration"))so.setCanalExfiltration(str(d,"canal_exfiltration"));
        if (d.containsKey("gravite"))           so.setGravite(toShort(d.get("gravite"),(short)2));
        if (d.containsKey("vraisemblance"))     so.setVraisemblance(toShort(d.get("vraisemblance"),(short)2));

        if (d.get("bien_support_id") != null)
            bsRepo.findById(Long.parseLong(d.get("bien_support_id").toString()))
                  .ifPresent(so::setBienSupport);

        so.setNiveauRisque(calcNiveauRisque(so.getAnalyse(), so.getGravite(), so.getVraisemblance()));
        return ResponseEntity.ok(soToMap(soRepo.save(so)));
    }

    @DeleteMapping("/scenarios-operationnels/{id}")
    public ResponseEntity<?> deleteSO(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));
        soRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // ACTIONS ÉLÉMENTAIRES
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/actions-elementaires")
    public ResponseEntity<?> createAE(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmScenarioOperationnel so = soRepo.findById(
            Long.parseLong(d.get("scenario_op_id").toString())).orElse(null);
        if (so == null) return ResponseEntity.badRequest().body(Map.of("error","Scénario opérationnel non trouvé"));

        short prob = toShort(d.get("probabilite_succes"),(short)2);
        short diff = toShort(d.get("difficulte"),(short)2);

        ErmActionElementaire ae = ErmActionElementaire.builder()
                .scenarioOp(so)
                .analyse(so.getAnalyse())
                .organism(so.getOrganism())
                .numero(toShort(d.get("numero"),(short)1))
                .libelle(str(d,"libelle"))
                .description(str(d,"description"))
                .modeOperatoire(d.getOrDefault("mode_operatoire","logique").toString())
                .probabiliteSucces(prob)
                .difficulte(diff)
                .vraisemblanceAction(calcVraisemblanceAction(prob, diff))
                .prerequis(str(d,"prerequis"))
                .build();

        return ResponseEntity.status(201).body(aeToMap(aeRepo.save(ae)));
    }

    @PutMapping("/actions-elementaires/{id}")
    public ResponseEntity<?> updateAE(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmActionElementaire ae = aeRepo.findById(id).orElse(null);
        if (ae == null) return ResponseEntity.notFound().build();

        if (d.containsKey("numero"))          ae.setNumero(toShort(d.get("numero"),(short)1));
        if (d.containsKey("libelle"))         ae.setLibelle(str(d,"libelle"));
        if (d.containsKey("description"))     ae.setDescription(str(d,"description"));
        if (d.containsKey("mode_operatoire")) ae.setModeOperatoire(str(d,"mode_operatoire"));
        if (d.containsKey("prerequis"))       ae.setPrerequisite(str(d,"prerequis"));

        if (d.containsKey("probabilite_succes")) ae.setProbabiliteSucces(toShort(d.get("probabilite_succes"),(short)2));
        if (d.containsKey("difficulte"))         ae.setDifficulte(toShort(d.get("difficulte"),(short)2));

        ae.setVraisemblanceAction(calcVraisemblanceAction(ae.getProbabiliteSucces(), ae.getDifficulte()));
        return ResponseEntity.ok(aeToMap(aeRepo.save(ae)));
    }

    @DeleteMapping("/actions-elementaires/{id}")
    public ResponseEntity<?> deleteAE(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));
        aeRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════════════

    private Map<String,Object> soToMap(ErmScenarioOperationnel s) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",                          s.getId());
        m.put("libelle",                     s.getLibelle());
        m.put("description",                 s.getDescription() != null ? s.getDescription() : "");
        m.put("canal_exfiltration",          s.getCanalExfiltration() != null ? s.getCanalExfiltration() : "");
        m.put("scenario_strategique_id",     s.getScenarioStrategique().getId());
        m.put("scenario_strategique_libelle",s.getScenarioStrategique().getLibelle());
        m.put("scenario_strategique_niveau", s.getScenarioStrategique().getNiveauRisque());
        m.put("bien_support_id",   s.getBienSupport() != null ? s.getBienSupport().getId()           : null);
        m.put("bien_support_nom",  s.getBienSupport() != null ? s.getBienSupport().getDenomination() : null);
        m.put("bien_support_type", s.getBienSupport() != null ? s.getBienSupport().getTypeBien()     : null);
        m.put("gravite",       s.getGravite());
        m.put("vraisemblance", s.getVraisemblance());
        m.put("niveau_risque", s.getNiveauRisque());
        m.put("score_risque",  s.getGravite() * s.getVraisemblance());
        m.put("niveau_label",  niveauLabel(s.getNiveauRisque()));
        return m;
    }

    private Map<String,Object> aeToMap(ErmActionElementaire ae) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",                   ae.getId());
        m.put("numero",               ae.getNumero());
        m.put("libelle",              ae.getLibelle());
        m.put("description",          ae.getDescription() != null ? ae.getDescription() : "");
        m.put("mode_operatoire",      ae.getModeOperatoire());
        m.put("probabilite_succes",   ae.getProbabiliteSucces());
        m.put("difficulte",           ae.getDifficulte());
        m.put("vraisemblance_action", ae.getVraisemblanceAction());
        m.put("prerequis",            ae.getPrerequisite() != null ? ae.getPrerequisite() : "");
        m.put("scenario_op_id",       ae.getScenarioOp().getId());
        return m;
    }

    private Map<String,Object> ssRefToMap(ErmScenarioStrategique s) {
        return Map.of("id",s.getId(),"libelle",s.getLibelle(),
                      "niveau_risque",s.getNiveauRisque() != null ? s.getNiveauRisque() : 0);
    }

    private Map<String,Object> bsRefToMap(ErmBienSupport b) {
        return Map.of("id",b.getId(),"denomination",b.getDenomination(),"type_bien",b.getTypeBien());
    }

    private String niveauLabel(Short n) {
        if (n == null) return "—";
        return switch(n) { case 1 -> "Faible"; case 2 -> "Moyen"; case 3 -> "Élevé"; default -> "—"; };
    }
}