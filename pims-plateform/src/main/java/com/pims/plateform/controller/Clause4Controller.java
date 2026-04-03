package com.pims.plateform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.entity.*;
import com.pims.plateform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/clause4")
@RequiredArgsConstructor
public class Clause4Controller {

    private final Clause4Repository    clause4Repository;
    private final OrganismRepository   organismRepository;
    private final UserRepository       userRepository;
    private final ObjectMapper         objectMapper = new ObjectMapper();

    private static final List<String> ROLES_RSSI = List.of("rssi", "super_admin");

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
        if (val == null || val.isBlank()) return List.of();
        try { return objectMapper.readValue(val, Object.class); }
        catch (Exception e) { return List.of(); }
    }

    private Map<String, Object> toMap(Clause4 c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           c.getId());
        m.put("organism_id",  c.getOrganism().getId());

        // 4.1
        m.put("enjeux_externes",       fromJson(c.getEnjeuxExternes()));
        m.put("enjeux_internes",       fromJson(c.getEnjeuxInternes()));

        // 4.2
        m.put("parties_interessees",   fromJson(c.getPartiesInteressees()));

        // 4.3
        m.put("perimetre_smsi",            c.getPerimetreSmsi());
        m.put("perimetre_pims",            c.getPerimetrePims());
        m.put("sites_concernes",           c.getSitesConcernes());
        m.put("activites_exclues",         c.getActivitesExclues());
        m.put("justification_exclusions",  c.getJustificationExclusions());
        m.put("interfaces_dependances",    c.getInterfacesDependances());
        m.put("responsable_traitement",    c.getResponsableTraitement());

        // 4.4
        m.put("engagement_direction",      c.getEngagementDirection());
        m.put("politique_securite",        c.getPolitiqueSecurite());
        m.put("politique_confidentialite", c.getPolitiqueConfidentialite());
        m.put("ressources_humaines",       fromJson(c.getRessourcesHumaines()));
        m.put("ressources_logicielles",    fromJson(c.getRessourcesLogicielles()));
        m.put("ressources_materielles",    fromJson(c.getRessourcesMaterielles()));
        m.put("procedures",                fromJson(c.getProcedures()));
        m.put("outils_protection",         fromJson(c.getOutilsProtection()));
        m.put("objectifs_smsi",            fromJson(c.getObjectifsSmsi()));
        m.put("date_revue",  c.getDateRevue()  != null ? c.getDateRevue().toString()  : null);
        m.put("version",     c.getVersion());
        m.put("statut",      c.getStatut());
        m.put("created_at",  c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        m.put("updated_at",  c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
        return m;
    }

    // ── GET /api/clause4 ──────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null)
            return ResponseEntity.ok(Map.of());

        Clause4 c = clause4Repository.findByOrganismId(orgId).orElse(null);
        if (c == null) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(toMap(c));
    }

    // ── POST /api/clause4 ─────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> save(@RequestBody Map<String, Object> data,
                                  @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Aucun organisme"));

        Organism org = organismRepository.findById(orgId).orElseThrow();
        Clause4 c = clause4Repository.findByOrganismId(orgId)
                .orElse(Clause4.builder().organism(org).build());

        // 4.1
        if (data.containsKey("enjeux_externes"))    c.setEnjeuxExternes(toJson(data.get("enjeux_externes")));
        if (data.containsKey("enjeux_internes"))    c.setEnjeuxInternes(toJson(data.get("enjeux_internes")));

        // 4.2
        if (data.containsKey("parties_interessees")) c.setPartiesInteressees(toJson(data.get("parties_interessees")));

        // 4.3
        if (data.containsKey("perimetre_smsi"))            c.setPerimetreSmsi(str(data, "perimetre_smsi"));
        if (data.containsKey("perimetre_pims"))            c.setPerimetrePims(str(data, "perimetre_pims"));
        if (data.containsKey("sites_concernes"))           c.setSitesConcernes(str(data, "sites_concernes"));
        if (data.containsKey("activites_exclues"))         c.setActivitesExclues(str(data, "activites_exclues"));
        if (data.containsKey("justification_exclusions"))  c.setJustificationExclusions(str(data, "justification_exclusions"));
        if (data.containsKey("interfaces_dependances"))    c.setInterfacesDependances(str(data, "interfaces_dependances"));
        if (data.containsKey("responsable_traitement"))    c.setResponsableTraitement(str(data, "responsable_traitement"));

        // 4.4
        if (data.containsKey("engagement_direction"))      c.setEngagementDirection(str(data, "engagement_direction"));
        if (data.containsKey("politique_securite"))        c.setPolitiqueSecurite(str(data, "politique_securite"));
        if (data.containsKey("politique_confidentialite")) c.setPolitiqueConfidentialite(str(data, "politique_confidentialite"));
        if (data.containsKey("ressources_humaines"))       c.setRessourcesHumaines(toJson(data.get("ressources_humaines")));
        if (data.containsKey("ressources_logicielles"))    c.setRessourcesLogicielles(toJson(data.get("ressources_logicielles")));
        if (data.containsKey("ressources_materielles"))    c.setRessourcesMaterielles(toJson(data.get("ressources_materielles")));
        if (data.containsKey("procedures"))                c.setProcedures(toJson(data.get("procedures")));
        if (data.containsKey("outils_protection"))         c.setOutilsProtection(toJson(data.get("outils_protection")));
        if (data.containsKey("objectifs_smsi"))            c.setObjectifsSmsi(toJson(data.get("objectifs_smsi")));
        if (data.containsKey("version"))                   c.setVersion(str(data, "version"));
        if (data.containsKey("date_revue") && data.get("date_revue") != null)
            c.setDateRevue(LocalDate.parse(str(data, "date_revue")));

        return ResponseEntity.ok(toMap(clause4Repository.save(c)));
    }

    private String str(Map<String, Object> data, String key) {
        return data.get(key) != null ? data.get(key).toString() : null;
    }
}