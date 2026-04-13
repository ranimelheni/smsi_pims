package com.pims.plateform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.entity.Organism;
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
public class EbiosAtelier1Controller {

    private final ErmAnalyseRepository          analyseRepo;
    private final ErmSocieteMissionRepository   societeRepo;
    private final ErmEntiteRepository           entiteRepo;
    private final ErmValeurMetierRepository     vmRepo;
    private final ErmBienSupportRepository      bsRepo;
    private final ErmEvenementRedouteRepository erRepo;
    private final ErmSocleSecuriteRepository    socleRepo;
    private final MethodologieRisqueRepository  methRepo;
    private final OrganismRepository            orgRepo;
    private final UserRepository                userRepo;
    private final ObjectMapper                  om = new ObjectMapper();

    private static final List<String> ROLES_RSSI = List.of("rssi","super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    private ErmAnalyse getAnalyse(Long orgId) {
        return analyseRepo.findByOrganismId(orgId).orElse(null);
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

    private String str(Map<String,Object> d, String k) {
        return d.get(k) != null ? d.get(k).toString() : null;
    }

    private Short toShort(Object v, short def) {
        if (v == null) return def;
        try { return Short.parseShort(v.toString()); }
        catch (Exception e) { return def; }
    }

    // ════════════════════════════════════════════════════════════════
    // INIT ANALYSE
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/analyse/init")
    public ResponseEntity<?> initAnalyse(
            @RequestBody(required = false) Map<String,Object> body,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        Long orgId = user.getOrganism().getId();

        // Vérifier méthode validée
        var meth = methRepo.findByOrganismId(orgId).orElse(null);
        if (meth == null || !"valide".equals(meth.getStatut()))
            return ResponseEntity.badRequest().body(Map.of(
                "error","La méthodologie EBIOS RM doit être validée par la direction avant de lancer l'analyse"));

        if (!"ebios_rm".equals(meth.getMethode()))
            return ResponseEntity.badRequest().body(Map.of(
                "error","La méthode sélectionnée n'est pas EBIOS RM"));

        if (analyseRepo.findByOrganismId(orgId).isPresent())
            return ResponseEntity.badRequest().body(Map.of("error","Analyse déjà initialisée"));

        Organism org = orgRepo.findById(orgId).orElseThrow();

        // Copier les paramètres depuis la méthodologie validée
        ErmAnalyse analyse = ErmAnalyse.builder()
                .organism(org)
                .createdBy(user)
                .titre(body != null && body.get("titre") != null
                    ? body.get("titre").toString() : "Analyse EBIOS RM")
                .seuilAcceptable(meth.getSeuilAcceptable())
                .seuilEleve(meth.getSeuilEleve())
                .echelleProbabilite(meth.getEchelleProbabilite())
                .echelleImpact(meth.getEchelleImpact())
                .labelsProbabilite(meth.getLabelsProbabilite())
                .labelsImpact(meth.getLabelsImpact())
                .build();

        analyseRepo.save(analyse);

        return ResponseEntity.status(201).body(Map.of(
            "message",          "Analyse EBIOS RM initialisée",
            "id",               analyse.getId(),
            "seuil_acceptable", analyse.getSeuilAcceptable(),
            "seuil_eleve",      analyse.getSeuilEleve(),
            "echelle_prob",     analyse.getEchelleProbabilite(),
            "echelle_impact",   analyse.getEchelleImpact()
        ));
    }

    // ── GET analyse ───────────────────────────────────────────────────────
    @GetMapping("/analyse")
    public ResponseEntity<?> getAnalyse(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism().getId();
        ErmAnalyse a = getAnalyse(orgId);
        if (a == null) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(analyseToMap(a));
    }

    // ════════════════════════════════════════════════════════════════
    // ATELIER 1 — GET COMPLET
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/atelier1")
    public ResponseEntity<?> getAtelier1(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism().getId();
        ErmAnalyse a = getAnalyse(orgId);
        if (a == null) return ResponseEntity.ok(Map.of("non_initialise", true));

        var societe  = societeRepo.findByAnalyseId(a.getId()).orElse(null);
        var entites  = entiteRepo.findByAnalyseIdOrderByNomEntite(a.getId());
        var vms      = vmRepo.findByAnalyseIdOrderByDenomination(a.getId());
        var bs       = bsRepo.findByAnalyseIdOrderByDenomination(a.getId());
        var ers      = erRepo.findByAnalyseIdOrderByGraviteDesc(a.getId());
        var socles   = socleRepo.findByAnalyseIdOrderByNomReferentiel(a.getId());

       Map<String, Object> response = new LinkedHashMap<>();

response.put("analyse", analyseToMap(a));
response.put("societe_mission", societe != null ? societeToMap(societe) : null);
response.put("entites", entites.stream().map(this::entiteToMap).toList());
response.put("valeurs_metier", vms.stream().map(this::vmToMap).toList());
response.put("biens_support", bs.stream().map(this::bsToMap).toList());
response.put("evenements_redoutes", ers.stream().map(this::erToMap).toList());
response.put("socle_securite", socles.stream().map(this::socleToMap).toList());

return ResponseEntity.ok(response);
    }

    // ════════════════════════════════════════════════════════════════
    // SOCIÉTÉ / MISSION
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/societe-mission")
    public ResponseEntity<?> saveSociete(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmSocieteMission sm = societeRepo.findByAnalyseId(a.getId())
                .orElse(ErmSocieteMission.builder().analyse(a).organism(user.getOrganism()).build());

        sm.setNomSociete(str(d,"nom_societe"));
        sm.setAdresse(str(d,"adresse"));
        sm.setContact(str(d,"contact"));
        sm.setMission(str(d,"mission"));

        return ResponseEntity.ok(societeToMap(societeRepo.save(sm)));
    }

    // ════════════════════════════════════════════════════════════════
    // ENTITÉS
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/entites")
    public ResponseEntity<?> createEntite(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        if (str(d,"nom_entite") == null || str(d,"nom_entite").isBlank())
            return ResponseEntity.badRequest().body(Map.of("error","Nom requis"));

        ErmEntite e = ErmEntite.builder()
                .analyse(a).organism(user.getOrganism())
                .nomEntite(str(d,"nom_entite"))
                .typeEntite(d.getOrDefault("type_entite","interne").toString())
                .responsable(str(d,"responsable"))
                .description(str(d,"description"))
                .build();

        return ResponseEntity.status(201).body(entiteToMap(entiteRepo.save(e)));
    }

    @PutMapping("/entites/{id}")
    public ResponseEntity<?> updateEntite(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        ErmEntite e = entiteRepo.findById(id).orElse(null);
        if (e == null) return ResponseEntity.notFound().build();

        if (d.containsKey("nom_entite"))   e.setNomEntite(str(d,"nom_entite"));
        if (d.containsKey("type_entite"))  e.setTypeEntite(str(d,"type_entite"));
        if (d.containsKey("responsable"))  e.setResponsable(str(d,"responsable"));
        if (d.containsKey("description"))  e.setDescription(str(d,"description"));

        return ResponseEntity.ok(entiteToMap(entiteRepo.save(e)));
    }

    @DeleteMapping("/entites/{id}")
    public ResponseEntity<?> deleteEntite(@PathVariable Long id) {
        entiteRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // VALEURS MÉTIER
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/valeurs-metier")
    public ResponseEntity<?> createVM(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmEntite entite = d.get("entite_id") != null
            ? entiteRepo.findById(Long.parseLong(d.get("entite_id").toString())).orElse(null) : null;

        ErmValeurMetier vm = ErmValeurMetier.builder()
                .analyse(a).organism(user.getOrganism())
                .entite(entite)
                .denomination(str(d,"denomination"))
                .mission(str(d,"mission"))
                .nature(d.getOrDefault("nature","processus").toString())
                .description(str(d,"description"))
                .besoinsSecurite(toJson(d.get("besoins_securite")))
                .build();

        return ResponseEntity.status(201).body(vmToMap(vmRepo.save(vm)));
    }

    @PutMapping("/valeurs-metier/{id}")
    public ResponseEntity<?> updateVM(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d) {

        ErmValeurMetier vm = vmRepo.findById(id).orElse(null);
        if (vm == null) return ResponseEntity.notFound().build();

        if (d.containsKey("denomination"))    vm.setDenomination(str(d,"denomination"));
        if (d.containsKey("mission"))         vm.setMission(str(d,"mission"));
        if (d.containsKey("nature"))          vm.setNature(str(d,"nature"));
        if (d.containsKey("description"))     vm.setDescription(str(d,"description"));
        if (d.containsKey("besoins_securite"))vm.setBesoinsSecurite(toJson(d.get("besoins_securite")));
        if (d.get("entite_id") != null)
            entiteRepo.findById(Long.parseLong(d.get("entite_id").toString()))
                      .ifPresent(vm::setEntite);

        return ResponseEntity.ok(vmToMap(vmRepo.save(vm)));
    }

    @DeleteMapping("/valeurs-metier/{id}")
    public ResponseEntity<?> deleteVM(@PathVariable Long id) {
        vmRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // BIENS SUPPORTS
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/biens-support")
    public ResponseEntity<?> createBS(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        if (d.get("valeur_metier_id") == null)
            return ResponseEntity.badRequest().body(Map.of("error","valeur_metier_id requis"));

        ErmValeurMetier vm = vmRepo.findById(
            Long.parseLong(d.get("valeur_metier_id").toString())).orElseThrow();

        ErmEntite entite = d.get("entite_id") != null
            ? entiteRepo.findById(Long.parseLong(d.get("entite_id").toString())).orElse(null) : null;

        ErmBienSupport bs = ErmBienSupport.builder()
                .analyse(a).organism(user.getOrganism())
                .valeurMetier(vm).entite(entite)
                .denomination(str(d,"denomination"))
                .typeBien(d.getOrDefault("type_bien","materiel").toString())
                .description(str(d,"description"))
                .responsable(str(d,"responsable"))
                .build();

        return ResponseEntity.status(201).body(bsToMap(bsRepo.save(bs)));
    }

    @PutMapping("/biens-support/{id}")
    public ResponseEntity<?> updateBS(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d) {

        ErmBienSupport bs = bsRepo.findById(id).orElse(null);
        if (bs == null) return ResponseEntity.notFound().build();

        if (d.containsKey("denomination")) bs.setDenomination(str(d,"denomination"));
        if (d.containsKey("type_bien"))    bs.setTypeBien(str(d,"type_bien"));
        if (d.containsKey("description"))  bs.setDescription(str(d,"description"));
        if (d.containsKey("responsable"))  bs.setResponsable(str(d,"responsable"));
        if (d.get("entite_id") != null)
            entiteRepo.findById(Long.parseLong(d.get("entite_id").toString()))
                      .ifPresent(bs::setEntite);

        return ResponseEntity.ok(bsToMap(bsRepo.save(bs)));
    }

    @DeleteMapping("/biens-support/{id}")
    public ResponseEntity<?> deleteBS(@PathVariable Long id) {
        bsRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // ÉVÉNEMENTS REDOUTÉS
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/evenements-redoutes")
    public ResponseEntity<?> createER(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmValeurMetier vm = vmRepo.findById(
            Long.parseLong(d.get("valeur_metier_id").toString())).orElseThrow();

        ErmEvenementRedoute er = ErmEvenementRedoute.builder()
                .analyse(a).organism(user.getOrganism())
                .valeurMetier(vm)
                .libelle(str(d,"libelle"))
                .gravite(toShort(d.get("gravite"),(short)2))
                .impacts(toJson(d.get("impacts")))
                .besoinsSecurite(toJson(d.get("besoins_securite")))
                .build();

        return ResponseEntity.status(201).body(erToMap(erRepo.save(er)));
    }

    @PutMapping("/evenements-redoutes/{id}")
    public ResponseEntity<?> updateER(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d) {

        ErmEvenementRedoute er = erRepo.findById(id).orElse(null);
        if (er == null) return ResponseEntity.notFound().build();

        if (d.containsKey("libelle"))          er.setLibelle(str(d,"libelle"));
        if (d.containsKey("gravite"))          er.setGravite(toShort(d.get("gravite"),(short)2));
        if (d.containsKey("impacts"))          er.setImpacts(toJson(d.get("impacts")));
        if (d.containsKey("besoins_securite")) er.setBesoinsSecurite(toJson(d.get("besoins_securite")));

        return ResponseEntity.ok(erToMap(erRepo.save(er)));
    }

    @DeleteMapping("/evenements-redoutes/{id}")
    public ResponseEntity<?> deleteER(@PathVariable Long id) {
        erRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // SOCLE SÉCURITÉ
    // ════════════════════════════════════════════════════════════════

    @PostMapping("/socle-securite")
    public ResponseEntity<?> createSocle(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        ErmAnalyse a = getAnalyse(user.getOrganism().getId());
        if (a == null) return ResponseEntity.badRequest().body(Map.of("error","Analyse non initialisée"));

        ErmSocleSecurite s = ErmSocleSecurite.builder()
                .analyse(a).organism(user.getOrganism())
                .nomReferentiel(str(d,"nom_referentiel"))
                .typeReferentiel(str(d,"type_referentiel"))
                .etatApplication(d.getOrDefault("etat_application","partiel").toString())
                .ecarts(toJson(d.get("ecarts")))
                .build();

        return ResponseEntity.status(201).body(socleToMap(socleRepo.save(s)));
    }

    @PutMapping("/socle-securite/{id}")
    public ResponseEntity<?> updateSocle(
            @PathVariable Long id,
            @RequestBody Map<String,Object> d) {

        ErmSocleSecurite s = socleRepo.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();

        if (d.containsKey("nom_referentiel"))  s.setNomReferentiel(str(d,"nom_referentiel"));
        if (d.containsKey("type_referentiel")) s.setTypeReferentiel(str(d,"type_referentiel"));
        if (d.containsKey("etat_application")) s.setEtatApplication(str(d,"etat_application"));
        if (d.containsKey("ecarts"))           s.setEcarts(toJson(d.get("ecarts")));

        return ResponseEntity.ok(socleToMap(socleRepo.save(s)));
    }

    @DeleteMapping("/socle-securite/{id}")
    public ResponseEntity<?> deleteSocle(@PathVariable Long id) {
        socleRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message","Supprimé"));
    }

    // ════════════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════════════

    private Map<String,Object> analyseToMap(ErmAnalyse a) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",      a.getId());
        m.put("titre",   a.getTitre());
        m.put("version", a.getVersion());
        m.put("statut",  a.getStatut());
        m.put("date_debut", a.getDateDebut() != null ? a.getDateDebut().toString() : null);
        m.put("date_fin",   a.getDateFin()   != null ? a.getDateFin().toString()   : null);
        m.put("seuil_acceptable",    a.getSeuilAcceptable());
        m.put("seuil_eleve",         a.getSeuilEleve());
        m.put("echelle_probabilite", a.getEchelleProbabilite());
        m.put("echelle_impact",      a.getEchelleImpact());
        m.put("labels_probabilite",  fromJson(a.getLabelsProbabilite()));
        m.put("labels_impact",       fromJson(a.getLabelsImpact()));
        m.put("created_at", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String,Object> societeToMap(ErmSocieteMission s) {
        return Map.of(
            "id",          s.getId(),
            "nom_societe", s.getNomSociete()  != null ? s.getNomSociete()  : "",
            "adresse",     s.getAdresse()     != null ? s.getAdresse()     : "",
            "contact",     s.getContact()     != null ? s.getContact()     : "",
            "mission",     s.getMission()     != null ? s.getMission()     : ""
        );
    }

    private Map<String,Object> entiteToMap(ErmEntite e) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",          e.getId());
        m.put("nom_entite",  e.getNomEntite());
        m.put("type_entite", e.getTypeEntite());
        m.put("responsable", e.getResponsable() != null ? e.getResponsable() : "");
        m.put("description", e.getDescription() != null ? e.getDescription() : "");
        return m;
    }

    private Map<String,Object> vmToMap(ErmValeurMetier v) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",              v.getId());
        m.put("denomination",    v.getDenomination());
        m.put("mission",         v.getMission()    != null ? v.getMission()    : "");
        m.put("nature",          v.getNature());
        m.put("description",     v.getDescription() != null ? v.getDescription() : "");
        m.put("besoins_securite",fromJson(v.getBesoinsSecurite()));
        m.put("entite_id",  v.getEntite() != null ? v.getEntite().getId()      : null);
        m.put("entite_nom", v.getEntite() != null ? v.getEntite().getNomEntite() : null);
        return m;
    }

    private Map<String,Object> bsToMap(ErmBienSupport b) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",               b.getId());
        m.put("denomination",     b.getDenomination());
        m.put("type_bien",        b.getTypeBien());
        m.put("description",      b.getDescription() != null ? b.getDescription() : "");
        m.put("responsable",      b.getResponsable() != null ? b.getResponsable() : "");
        m.put("valeur_metier_id", b.getValeurMetier().getId());
        m.put("valeur_metier_nom",b.getValeurMetier().getDenomination());
        m.put("entite_id",  b.getEntite() != null ? b.getEntite().getId()      : null);
        m.put("entite_nom", b.getEntite() != null ? b.getEntite().getNomEntite() : null);
        return m;
    }

    private Map<String,Object> erToMap(ErmEvenementRedoute er) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",              er.getId());
        m.put("libelle",         er.getLibelle());
        m.put("gravite",         er.getGravite());
        m.put("gravite_label",   graviteLabel(er.getGravite()));
        m.put("impacts",         fromJson(er.getImpacts()));
        m.put("besoins_securite",fromJson(er.getBesoinsSecurite()));
        m.put("valeur_metier_id", er.getValeurMetier().getId());
        m.put("valeur_metier_nom",er.getValeurMetier().getDenomination());
        return m;
    }

    private Map<String,Object> socleToMap(ErmSocleSecurite s) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",               s.getId());
        m.put("nom_referentiel",  s.getNomReferentiel());
        m.put("type_referentiel", s.getTypeReferentiel() != null ? s.getTypeReferentiel() : "");
        m.put("etat_application", s.getEtatApplication());
        m.put("ecarts",           fromJson(s.getEcarts()));
        return m;
    }

    private String graviteLabel(Short g) {
        if (g == null) return "—";
        return switch (g) {
            case 1 -> "Négligeable";
            case 2 -> "Limitée";
            case 3 -> "Importante";
            case 4 -> "Critique";
            default -> "—";
        };
    }
}