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
@RequestMapping("/api/soa")
@RequiredArgsConstructor
public class SoaImplementationController {

    private final SoaControleImplementationRepository implRepo;
    private final SoaControleRepository               controleRepo;
    private final SoaRepository                       soaRepo;
 
    private final UserRepository                      userRepo;
    private final ObjectMapper                        om = new ObjectMapper();

    private static final List<String> ROLES_RSSI = List.of("rssi","super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername())).orElseThrow();
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

    private Map<String,Object> toMap(SoaControleImplementation i) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",              i.getId());
        m.put("soa_controle_id", i.getSoaControle().getId());
        m.put("controle_id",     i.getSoaControle().getControleId());
        m.put("controle_label",  i.getSoaControle().getControleLabel());
        m.put("annexe",          i.getSoaControle().getAnnexe());
        m.put("statut_detail",   i.getStatutDetail());
        m.put("niveau_maturite", i.getNiveauMaturite());
        m.put("outils",          fromJson(i.getOutils()));
        m.put("regles_gestion",  fromJson(i.getReglesGestion()));
        m.put("procedures",      fromJson(i.getProcedures()));
        m.put("configurations",  fromJson(i.getConfigurations()));
        m.put("preuves",         fromJson(i.getPreuves()));
        m.put("notes",           i.getNotes() != null ? i.getNotes() : "");
        m.put("responsable",     i.getResponsable() != null ? i.getResponsable() : "");
        m.put("date_revue",      i.getDateRevue() != null ? i.getDateRevue().toString() : null);
        m.put("updated_at",      i.getUpdatedAt() != null ? i.getUpdatedAt().toString() : null);
        return m;
    }

    // ── GET /api/soa/implementations ──────────────────────────────────────────
    // Tous les détails d'implémentation pour la SoA validée de l'organisme
    @GetMapping("/implementations")
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null) return ResponseEntity.ok(List.of());
        

        Soa soa = soaRepo.findByOrganismId(orgId).orElse(null);
        if (soa == null) return ResponseEntity.ok(Map.of("error","SoA non initialisée"));
if (!"valide".equalsIgnoreCase(soa.getStatut())) {
    return ResponseEntity.ok(Map.of(
        "error", "SoA non validée par la direction",
        "statut", soa.getStatut()
    ));
}
        // Récupérer uniquement les contrôles inclus
        List<SoaControle> applicables = controleRepo
                .findBySoa_IdOrderByControleId(soa.getId())
                .stream()
                .filter(SoaControle::getInclus)
                .toList();

        // Récupérer implémentations existantes
        List<SoaControleImplementation> impls = implRepo.findBySoaId(soa.getId());
        Map<Long, SoaControleImplementation> implByControleId = new HashMap<>();
        impls.forEach(i -> implByControleId.put(i.getSoaControle().getId(), i));

        // Stats
        long complet    = implRepo.countByOrganismIdAndStatutDetail(orgId,"complet");
        long partiel    = implRepo.countByOrganismIdAndStatutDetail(orgId,"partiel");
        long enCours    = implRepo.countByOrganismIdAndStatutDetail(orgId,"en_cours");
        long nonCommence= implRepo.countByOrganismIdAndStatutDetail(orgId,"non_commence");

        // Grouper par annexe avec statut implémentation
        Map<String,List<Map<String,Object>>> parAnnexe = new LinkedHashMap<>();
        applicables.forEach(c -> {
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("controle_id",    c.getId());
            item.put("controle_ref",   c.getControleId());
            item.put("controle_label", c.getControleLabel());
            item.put("description",    c.getDescription());
            item.put("annexe",         c.getAnnexe());
            item.put("statut_impl",    c.getStatutImpl());
            item.put("responsable_soa",c.getResponsable());

            SoaControleImplementation impl = implByControleId.get(c.getId());
            if (impl != null) {
                item.put("implementation", toMap(impl));
            } else {
                item.put("implementation", null);
            }
            parAnnexe.computeIfAbsent(c.getAnnexe(), k -> new ArrayList<>()).add(item);
        });

        return ResponseEntity.ok(Map.of(
            "soa_statut",  soa.getStatut(),
            "audit_type",  soa.getAuditType(),
            "total_applicables", applicables.size(),
            "stats", Map.of(
                "complet",     complet,
                "partiel",     partiel,
                "en_cours",    enCours,
                "non_commence",nonCommence,
                "taux",        applicables.isEmpty() ? 0 : (complet * 100 / applicables.size())
            ),
            "par_annexe",  parAnnexe
        ));
        
    }

    // ── GET /api/soa/implementations/{controleId} ─────────────────────────────
    @GetMapping("/implementations/controle/{controleId}")
    public ResponseEntity<?> getByControle(
            @PathVariable Long controleId,
            @AuthenticationPrincipal UserDetails ud) {

        SoaControleImplementation impl = implRepo.findBySoaControleId(controleId).orElse(null);
        if (impl == null) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(toMap(impl));
    }

    // ── POST /api/soa/implementations/{controleId} ────────────────────────────
    @PostMapping("/implementations/controle/{controleId}")
    public ResponseEntity<?> save(
            @PathVariable Long controleId,
            @RequestBody Map<String,Object> data,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Réservé au RSSI"));

        SoaControle controle = controleRepo.findById(controleId).orElse(null);
        if (controle == null) return ResponseEntity.notFound().build();

        if (!controle.getInclus())
            return ResponseEntity.badRequest()
                    .body(Map.of("error","Ce contrôle est marqué non applicable"));

        Organism org = user.getOrganism();
        SoaControleImplementation impl = implRepo.findBySoaControleId(controleId)
                .orElse(SoaControleImplementation.builder()
                        .soaControle(controle)
                        .organism(org)
                        .build());

        if (data.containsKey("statut_detail"))  impl.setStatutDetail(str(data,"statut_detail"));
        if (data.containsKey("niveau_maturite"))
            impl.setNiveauMaturite(Short.parseShort(data.get("niveau_maturite").toString()));
        if (data.containsKey("outils"))         impl.setOutils(toJson(data.get("outils")));
        if (data.containsKey("regles_gestion")) impl.setReglesGestion(toJson(data.get("regles_gestion")));
        if (data.containsKey("procedures"))     impl.setProcedures(toJson(data.get("procedures")));
        if (data.containsKey("configurations")) impl.setConfigurations(toJson(data.get("configurations")));
        if (data.containsKey("preuves"))        impl.setPreuves(toJson(data.get("preuves")));
        if (data.containsKey("notes"))          impl.setNotes(str(data,"notes"));
        if (data.containsKey("responsable"))    impl.setResponsable(str(data,"responsable"));
        if (data.containsKey("date_revue") && data.get("date_revue") != null)
            impl.setDateRevue(LocalDate.parse(str(data,"date_revue")));

        return ResponseEntity.ok(toMap(implRepo.save(impl)));
    }
}