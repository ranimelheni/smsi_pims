package com.pims.plateform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.entity.*;
import com.pims.plateform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/methodologie-risque")
@RequiredArgsConstructor
public class MethodologieRisqueController {

    private final MethodologieRisqueRepository methRepo;
    private final OrganismRepository           organismRepository;
    private final UserRepository               userRepository;
    private final ObjectMapper                 objectMapper = new ObjectMapper();

    private static final List<String> ROLES_RSSI      = List.of("rssi", "super_admin");
    private static final List<String> ROLES_DIRECTION  = List.of("direction", "super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    private String toJson(Object val) {
        if (val == null) return "[]";
        if (val instanceof String s) return s;
        try { return objectMapper.writeValueAsString(val); }
        catch (Exception e) { return "[]"; }
    }

    private Object fromJson(String val) {
        if (val == null) return List.of();
        try { return objectMapper.readValue(val, Object.class); }
        catch (Exception e) { return List.of(); }
    }

    private Map<String, Object> toMap(MethodologieRisque m) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id",                    m.getId());
        r.put("organism_id",           m.getOrganism().getId());
        r.put("methode",               m.getMethode());
        r.put("methode_custom",        m.getMethodeCustom());
        r.put("echelle_probabilite",   m.getEchelleProbabilite());
        r.put("echelle_impact",        m.getEchelleImpact());
        r.put("labels_probabilite",    fromJson(m.getLabelsProbabilite()));
        r.put("labels_impact",         fromJson(m.getLabelsImpact()));
        r.put("seuil_acceptable",      m.getSeuilAcceptable());
        r.put("seuil_eleve",           m.getSeuilEleve());
        r.put("formule_calcul",        m.getFormuleCalcul());
        r.put("justification",         m.getJustification());
        r.put("perimetre_risque",      m.getPerimetreRisque());
        r.put("objectifs_risque",      m.getObjectifsRisque());
        r.put("criteres_acceptation",  m.getCriteresAcceptation());
        r.put("statut",                m.getStatut());
        r.put("commentaire_direction", m.getCommentaireDirection());
        r.put("valide_by",  m.getValidePar()  != null
            ? m.getValidePar().getPrenom()  + " " + m.getValidePar().getNom()  : null);
        r.put("propose_by", m.getProposePar() != null
            ? m.getProposePar().getPrenom() + " " + m.getProposePar().getNom() : null);
        r.put("valide_at",  m.getValideAt()   != null ? m.getValideAt().toString()  : null);
        r.put("updated_at", m.getUpdatedAt()  != null ? m.getUpdatedAt().toString() : null);
        // Après r.put("updated_at", ...)
r.put("type_audit", 
    m.getOrganism().getAuditType() != null 
    ? m.getOrganism().getAuditType().name().toLowerCase().replace("_","") 
    : "iso27001");
        // Matrice de risque calculée
        r.put("matrice", buildMatrice(m));
        return r;
    }

    private List<List<String>> buildMatrice(MethodologieRisque m) {
        int p = m.getEchelleProbabilite();
        int i = m.getEchelleImpact();
        int seuilAcc  = m.getSeuilAcceptable();
        int seuilElev = m.getSeuilEleve();
        List<List<String>> matrice = new ArrayList<>();
        for (int pi = p; pi >= 1; pi--) {
            List<String> row = new ArrayList<>();
            for (int ii = 1; ii <= i; ii++) {
                int score = pi * ii; // formule P×I
                String niveau;
                if (score <= seuilAcc)       niveau = "faible";
                else if (score <= seuilElev) niveau = "moyen";
                else                         niveau = "eleve";
                row.add(niveau + ":" + score);
            }
            matrice.add(row);
        }
        return matrice;
    }

    // ── GET /api/methodologie-risque ──────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal UserDetails ud) {
        User user  = getCurrentUser(ud);
        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null) return ResponseEntity.ok(Map.of());

        MethodologieRisque m = methRepo.findByOrganismId(orgId).orElse(null);
        if (m == null) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(toMap(m));
    }

    // ── POST /api/methodologie-risque ─────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> save(@RequestBody Map<String, Object> data,
                                   @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Long orgId = user.getOrganism().getId();
        Organism org = organismRepository.findById(orgId).orElseThrow();

// Après : Organism org = organismRepository.findById(orgId).orElseThrow();
// Ajouter :
String typeAudit = org.getAuditType() != null 
    ? org.getAuditType().name().toLowerCase().replace("_", "") 
    : "iso27001";

MethodologieRisque m = methRepo.findByOrganismId(orgId)
        .orElse(MethodologieRisque.builder()
            .organism(org)
            .proposePar(user)
            // Par défaut : PIA pour 27701, EBIOS pour 27001
            .methode("iso27701".equals(typeAudit) ? "pia" : "ebios_rm")
            .build());

// Bloquer le changement de méthode vers non-PIA si 27701
if ("iso27701".equals(typeAudit)) {
    m.setMethode("pia"); // forcé
    if (data.containsKey("justification"))
        m.setJustification(str(data, "justification"));
    if (data.containsKey("perimetre_risque"))
        m.setPerimetreRisque(str(data, "perimetre_risque"));
    if (data.containsKey("objectifs_risque"))
        m.setObjectifsRisque(str(data, "objectifs_risque"));
    if (data.containsKey("criteres_acceptation"))
        m.setCriteresAcceptation(str(data, "criteres_acceptation"));
    // PIA : paramètres spécifiques
    if (data.containsKey("pia_categories_donnees"))
        m.setJustification(str(data, "justification")); // réutiliser le champ
} else {
    // ISO 27001 — logique existante
    if (data.containsKey("methode"))         m.setMethode(str(data, "methode"));
    if (data.containsKey("methode_custom"))  m.setMethodeCustom(str(data, "methode_custom"));
    if (data.containsKey("justification"))   m.setJustification(str(data, "justification"));
    if (data.containsKey("perimetre_risque"))m.setPerimetreRisque(str(data, "perimetre_risque"));
    if (data.containsKey("objectifs_risque"))m.setObjectifsRisque(str(data, "objectifs_risque"));
    if (data.containsKey("criteres_acceptation"))
        m.setCriteresAcceptation(str(data, "criteres_acceptation"));
    if (data.containsKey("echelle_probabilite"))
        m.setEchelleProbabilite(parseInt(data.get("echelle_probabilite"), 4));
    if (data.containsKey("echelle_impact"))
        m.setEchelleImpact(parseInt(data.get("echelle_impact"), 4));
    if (data.containsKey("seuil_acceptable"))
        m.setSeuilAcceptable(parseInt(data.get("seuil_acceptable"), 6));
    if (data.containsKey("seuil_eleve"))
        m.setSeuilEleve(parseInt(data.get("seuil_eleve"), 12));
    if (data.containsKey("formule_calcul"))
        m.setFormuleCalcul(str(data, "formule_calcul"));
    if (data.containsKey("labels_probabilite"))
        m.setLabelsProbabilite(toJson(data.get("labels_probabilite")));
    if (data.containsKey("labels_impact"))
        m.setLabelsImpact(toJson(data.get("labels_impact")));
}

if ("rejete".equals(m.getStatut())) m.setStatut("propose");
return ResponseEntity.ok(toMap(methRepo.save(m)));
    }

    // ── PUT /api/methodologie-risque/soumettre ────────────────────────────────
    @PutMapping("/soumettre")
    public ResponseEntity<?> soumettre(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Long orgId = user.getOrganism().getId();
        MethodologieRisque m = methRepo.findByOrganismId(orgId).orElse(null);
        if (m == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Méthodologie non définie"));

        m.setStatut("soumis_direction");
        methRepo.save(m);
        return ResponseEntity.ok(Map.of("statut", "soumis_direction",
            "message", "Méthodologie soumise à la direction"));
    }

    // ── PUT /api/methodologie-risque/valider ──────────────────────────────────
    @PutMapping("/valider")
    public ResponseEntity<?> valider(@RequestBody Map<String, Object> data,
                                      @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_DIRECTION.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à la direction"));

        Long orgId = user.getOrganism().getId();
        MethodologieRisque m = methRepo.findByOrganismId(orgId).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();

        String decision = str(data, "decision"); // valide | rejete
        m.setStatut("valide".equals(decision) ? "valide" : "rejete");
        m.setValidePar(user);
        m.setValideAt(LocalDateTime.now());
        m.setCommentaireDirection(str(data, "commentaire"));
        methRepo.save(m);

        return ResponseEntity.ok(Map.of("statut", m.getStatut()));
    }

    private String str(Map<String, Object> d, String k) {
        return d.get(k) != null ? d.get(k).toString() : null;
    }
    private int parseInt(Object v, int def) {
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; }
    }
}