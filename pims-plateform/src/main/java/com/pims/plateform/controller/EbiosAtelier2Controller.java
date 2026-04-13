// EbiosAtelier2Controller.java
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
public class EbiosAtelier2Controller {

    private final ErmAnalyseRepository      analyseRepo;
    private final ErmSourceRisqueRepository  srRepo;
    private final ErmObjectifViseRepository  ovRepo;
    private final UserRepository             userRepo;

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

    // ════════════════════════════════════════════════════════════════
    // GET ATELIER 2 COMPLET
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/atelier2")
    public ResponseEntity<?> getAtelier2(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.ok(Map.of("non_initialise", true));

        List<ErmSourceRisque>  sources   = srRepo.findByAnalyseIdOrderByLibelle(a.getId());
        List<ErmObjectifVise>  objectifs = ovRepo.findByAnalyseIdOrderBySourceIdAscLibelleAsc(a.getId());

        // Grouper objectifs par source
        Map<Long, List<Map<String,Object>>> ovParSource = new LinkedHashMap<>();
        sources.forEach(s -> ovParSource.put(s.getId(), new ArrayList<>()));
        objectifs.forEach(ov ->
            ovParSource.computeIfAbsent(ov.getSource().getId(), k -> new ArrayList<>())
                       .add(ovToMap(ov)));

        long srRetenus = srRepo.countByAnalyseIdAndRetenuTrue(a.getId());
        long ovRetenus = ovRepo.countByAnalyseIdAndRetenuTrue(a.getId());

        return ResponseEntity.ok(Map.of(
            "sources_risque",   sources.stream().map(this::srToMap).toList(),
            "objectifs_vises",  objectifs.stream().map(this::ovToMap).toList(),
            "ov_par_source",    ovParSource,
            "stats", Map.of(
                "sources_total",   sources.size(),
                "sources_retenues",srRetenus,
                "couples_total",   objectifs.size(),
                "couples_retenus", ovRetenus
            )
        ));
    }

    // ════════════════════════════════════════════════════════════════
    // SOURCES DE RISQUE
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/sources-risque")
    public ResponseEntity<?> createSR(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        if (str(d,"libelle") == null || str(d,"libelle").isBlank())
            return ResponseEntity.badRequest().body(Map.of("error","Libellé requis"));

        ErmSourceRisque sr = ErmSourceRisque.builder()
                .analyse(a).organism(user.getOrganism())
                .libelle(str(d,"libelle"))
                .categorie(d.getOrDefault("categorie","externe").toString())
                .description(str(d,"description"))
                .retenu(d.get("retenu") == null || Boolean.parseBoolean(d.get("retenu").toString()))
                .build();

        return ResponseEntity.status(201).body(srToMap(srRepo.save(sr)));
    }

    @PutMapping("/sources-risque/{id}")
    public ResponseEntity<?> updateSR(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmSourceRisque sr = srRepo.findById(id).orElse(null);
        if (sr == null) return ResponseEntity.notFound().build();

        if (d.containsKey("libelle"))     sr.setLibelle(str(d,"libelle"));
        if (d.containsKey("categorie"))   sr.setCategorie(str(d,"categorie"));
        if (d.containsKey("description")) sr.setDescription(str(d,"description"));
        if (d.containsKey("retenu"))      sr.setRetenu(Boolean.parseBoolean(d.get("retenu").toString()));

        return ResponseEntity.ok(srToMap(srRepo.save(sr)));
    }

    @DeleteMapping("/sources-risque/{id}")
    public ResponseEntity<?> deleteSR(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));
        srRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // OBJECTIFS VISÉS (couples SR/OV)
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/objectifs-vises")
    public ResponseEntity<?> createOV(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        if (d.get("source_id") == null)
            return ResponseEntity.badRequest().body(Map.of("error","source_id requis"));

        ErmSourceRisque sr = srRepo.findById(
            Long.parseLong(d.get("source_id").toString())).orElse(null);
        if (sr == null) return ResponseEntity.badRequest().body(Map.of("error","Source non trouvée"));

        Short pp = toShort(d.get("pertinence_proposee"),(short)2);
        Short pr = d.get("pertinence_retenue") != null
                   ? toShort(d.get("pertinence_retenue"),(short)2) : pp;
        boolean retenu = d.get("retenu") == null || Boolean.parseBoolean(d.get("retenu").toString());

        ErmObjectifVise ov = ErmObjectifVise.builder()
                .analyse(a).organism(user.getOrganism())
                .source(sr)
                .libelle(str(d,"libelle"))
                .description(str(d,"description"))
                .motivation(toShort(d.get("motivation"),(short)2))
                .ressource(toShort(d.get("ressource"),(short)2))
                .activite(toShort(d.get("activite"),(short)2))
                .pertinenceProposee(pp)
                .pertinenceRetenue(pr)
                .retenu(retenu)
                .justificationRejet(retenu ? null : str(d,"justification_rejet"))
                .build();

        return ResponseEntity.status(201).body(ovToMap(ovRepo.save(ov)));
    }

    @PutMapping("/objectifs-vises/{id}")
    public ResponseEntity<?> updateOV(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmObjectifVise ov = ovRepo.findById(id).orElse(null);
        if (ov == null) return ResponseEntity.notFound().build();

        if (d.containsKey("libelle"))              ov.setLibelle(str(d,"libelle"));
        if (d.containsKey("description"))          ov.setDescription(str(d,"description"));
        if (d.containsKey("motivation"))           ov.setMotivation(toShort(d.get("motivation"),(short)2));
        if (d.containsKey("ressource"))            ov.setRessource(toShort(d.get("ressource"),(short)2));
        if (d.containsKey("activite"))             ov.setActivite(toShort(d.get("activite"),(short)2));
        if (d.containsKey("pertinence_proposee"))  ov.setPertinenceProposee(toShort(d.get("pertinence_proposee"),(short)2));
        if (d.containsKey("pertinence_retenue"))   ov.setPertinenceRetenue(toShort(d.get("pertinence_retenue"),(short)2));
        if (d.containsKey("retenu")) {
            boolean retenu = Boolean.parseBoolean(d.get("retenu").toString());
            ov.setRetenu(retenu);
            if (!retenu && d.containsKey("justification_rejet"))
                ov.setJustificationRejet(str(d,"justification_rejet"));
            if (retenu) ov.setJustificationRejet(null);
        }

        return ResponseEntity.ok(ovToMap(ovRepo.save(ov)));
    }

    @DeleteMapping("/objectifs-vises/{id}")
    public ResponseEntity<?> deleteOV(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));
        ovRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════════════

    private Map<String,Object> srToMap(ErmSourceRisque s) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",          s.getId());
        m.put("libelle",     s.getLibelle());
        m.put("categorie",   s.getCategorie());
        m.put("description", s.getDescription() != null ? s.getDescription() : "");
        m.put("retenu",      s.getRetenu());
        return m;
    }

    private Map<String,Object> ovToMap(ErmObjectifVise o) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",                  o.getId());
        m.put("libelle",             o.getLibelle());
        m.put("description",         o.getDescription() != null ? o.getDescription() : "");
        m.put("source_id",           o.getSource().getId());
        m.put("source_libelle",      o.getSource().getLibelle());
        m.put("source_categorie",    o.getSource().getCategorie());
        m.put("motivation",          o.getMotivation());
        m.put("ressource",           o.getRessource());
        m.put("activite",            o.getActivite());
        m.put("pertinence_proposee", o.getPertinenceProposee());
        m.put("pertinence_retenue",  o.getPertinenceRetenue());
        m.put("retenu",              o.getRetenu());
        m.put("justification_rejet", o.getJustificationRejet() != null ? o.getJustificationRejet() : "");
        // Calcul pertinence globale = max(motivation, ressource, activite)
        m.put("pertinence_calculee", Math.max(o.getMotivation(),
            Math.max(o.getRessource(), o.getActivite())));
        return m;
    }
}