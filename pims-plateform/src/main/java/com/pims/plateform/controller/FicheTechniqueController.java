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
@RequestMapping("/api/fiches-techniques")
@RequiredArgsConstructor
public class FicheTechniqueController {

    private final FicheTechniqueRepository ficheRepo;
    private final UserRepository           userRepository;
    private final OrganismRepository       organismRepository;
    private final ObjectMapper             objectMapper = new ObjectMapper();

    private static final List<String> ROLES_TECH = List.of(
         "membre_equipe_technique"
    );
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

    private Map<String, Object> toMap(FicheTechnique f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           f.getId());
        m.put("organism_id",  f.getOrganism().getId());
        m.put("acteur_id",    f.getActeur().getId());
        m.put("acteur_nom",   f.getActeur().getPrenom() + " " + f.getActeur().getNom());
        m.put("acteur_role",  f.getActeur().getRole());
        m.put("intitule",     f.getIntitule());
        m.put("perimetre",    f.getPerimetre());

        // Actifs matériels
        m.put("actifs_serveurs",      fromJson(f.getActifsServeurs()));
        m.put("actifs_postes",        fromJson(f.getActifsPostes()));
        m.put("actifs_reseau",        fromJson(f.getActifsReseau()));

        // Actifs logiciels
        m.put("actifs_applications",  fromJson(f.getActifsApplications()));
        m.put("actifs_licences",      fromJson(f.getActifsLicences()));
        m.put("actifs_bdd",           fromJson(f.getActifsBdd()));

        // Actifs données
        m.put("actifs_sauvegardes",   fromJson(f.getActifsSauvegardes()));
        m.put("actifs_stockages",     fromJson(f.getActifsStockages()));

        // Actifs services
        m.put("actifs_cloud",         fromJson(f.getActifsCloud()));
        m.put("actifs_acces",         fromJson(f.getActifsAcces()));
        m.put("actifs_certificats",   fromJson(f.getActifsCertificats()));

        // Mesures
        m.put("mesures_securite",     fromJson(f.getMesuresSecurite()));

        // Workflow
        m.put("statut",               f.getStatut());
        m.put("commentaire_rejet",    f.getCommentaireRejet());
        m.put("valide_by",            f.getValidePar() != null ? f.getValidePar().getId() : null);
        m.put("valide_at",            f.getValideAt()  != null ? f.getValideAt().toString() : null);
        m.put("soumis_at",            f.getSoumisAt()  != null ? f.getSoumisAt().toString() : null);
        m.put("created_at",           f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
        m.put("updated_at",           f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : null);
        return m;
    }

    // ── GET /api/fiches-techniques/mine ───────────────────────────────────────
    @GetMapping("/mine")
    public ResponseEntity<?> getMine(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_TECH.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Aucun organisme associé"));

        FicheTechnique fiche = ficheRepo
                .findByOrganismIdAndActeurId(orgId, user.getId())
                .orElse(null);

        if (fiche == null) {
            Organism org = organismRepository.findById(orgId).orElseThrow();
            fiche = FicheTechnique.builder()
                    .organism(org)
                    .acteur(user)
                    .intitule("Fiche technique — " + user.getPrenom() + " " + user.getNom())
                    .statut("brouillon")
                    .build();
            ficheRepo.save(fiche);
            fiche = ficheRepo.findByOrganismIdAndActeurId(orgId, user.getId()).orElse(fiche);
        }

        return ResponseEntity.ok(toMap(fiche));
    }

    // ── PUT /api/fiches-techniques/{id} ───────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        FicheTechnique fiche = ficheRepo.findById(id).orElse(null);
        if (fiche == null) return ResponseEntity.notFound().build();

        boolean isTech = ROLES_TECH.contains(user.getRole());
        boolean isRssi = ROLES_RSSI.contains(user.getRole());

        if (!isTech && !isRssi)
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        // L'équipe technique ne peut modifier que ses propres fiches en brouillon
        if (isTech && !fiche.getActeur().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Ce n'est pas votre fiche"));
        if (isTech && !"brouillon".equals(fiche.getStatut()) && !"rejete".equals(fiche.getStatut()))
            return ResponseEntity.badRequest().body(Map.of("error", "Fiche non modifiable"));

        // Champs texte
        if (data.containsKey("intitule"))  fiche.setIntitule(data.get("intitule").toString());
        if (data.containsKey("perimetre")) fiche.setPerimetre(data.get("perimetre").toString());

        // Actifs matériels
        if (data.containsKey("actifs_serveurs"))     fiche.setActifsServeurs(toJson(data.get("actifs_serveurs")));
        if (data.containsKey("actifs_postes"))       fiche.setActifsPostes(toJson(data.get("actifs_postes")));
        if (data.containsKey("actifs_reseau"))       fiche.setActifsReseau(toJson(data.get("actifs_reseau")));

        // Actifs logiciels
        if (data.containsKey("actifs_applications")) fiche.setActifsApplications(toJson(data.get("actifs_applications")));
        if (data.containsKey("actifs_licences"))     fiche.setActifsLicences(toJson(data.get("actifs_licences")));
        if (data.containsKey("actifs_bdd"))          fiche.setActifsBdd(toJson(data.get("actifs_bdd")));

        // Actifs données
        if (data.containsKey("actifs_sauvegardes"))  fiche.setActifsSauvegardes(toJson(data.get("actifs_sauvegardes")));
        if (data.containsKey("actifs_stockages"))    fiche.setActifsStockages(toJson(data.get("actifs_stockages")));

        // Actifs services
        if (data.containsKey("actifs_cloud"))        fiche.setActifsCloud(toJson(data.get("actifs_cloud")));
        if (data.containsKey("actifs_acces"))        fiche.setActifsAcces(toJson(data.get("actifs_acces")));
        if (data.containsKey("actifs_certificats"))  fiche.setActifsCertificats(toJson(data.get("actifs_certificats")));

        // Mesures
        if (data.containsKey("mesures_securite"))    fiche.setMesuresSecurite(toJson(data.get("mesures_securite")));

ficheRepo.save(fiche);

FicheTechnique ficheLoaded = ficheRepo.findByIdWithActeur(id).orElseThrow();

return ResponseEntity.ok(toMap(ficheLoaded));    }

    // ── PUT /api/fiches-techniques/{id}/statut ────────────────────────────────
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> updateStatut(@PathVariable Long id,
                                          @RequestBody Map<String, Object> data,
                                          @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        FicheTechnique fiche = ficheRepo.findById(id).orElse(null);
        if (fiche == null) return ResponseEntity.notFound().build();

        String statut = data.get("statut").toString();

        // Équipe technique soumet
        if (ROLES_TECH.contains(user.getRole())) {
            if (!fiche.getActeur().getId().equals(user.getId()))
                return ResponseEntity.status(403).body(Map.of("error", "Ce n'est pas votre fiche"));
            if (!List.of("brouillon", "rejete").contains(fiche.getStatut()))
                return ResponseEntity.badRequest().body(Map.of("error", "Fiche déjà soumise"));
            fiche.setStatut("soumis_rssi");
            fiche.setSoumisAt(LocalDateTime.now());
            ficheRepo.save(fiche);
            return ResponseEntity.ok(Map.of("statut", "soumis_rssi", "message", "Fiche soumise au RSSI"));
        }

        // RSSI valide ou rejette
        if (ROLES_RSSI.contains(user.getRole())) {
            if (!"soumis_rssi".equals(fiche.getStatut()))
                return ResponseEntity.badRequest().body(Map.of("error", "Fiche non soumise"));
            if (!List.of("valide", "rejete").contains(statut))
                return ResponseEntity.badRequest().body(Map.of("error", "Statut invalide"));

            fiche.setStatut(statut);
            fiche.setValidePar(user);
            fiche.setValideAt(LocalDateTime.now());
            if ("rejete".equals(statut))
                fiche.setCommentaireRejet(data.getOrDefault("commentaire", "").toString());
            else
                fiche.setCommentaireRejet(null);

            ficheRepo.save(fiche);
            return ResponseEntity.ok(Map.of("statut", statut, "message", "Fiche " + statut));
        }

        return ResponseEntity.status(403).body(Map.of("error", "Action non autorisée"));
    }

    // ── GET /api/fiches-techniques (RSSI — toutes les fiches organisme) ───────
    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Aucun organisme"));

        List<FicheTechnique> fiches = ficheRepo.findByOrganismId(orgId);
        return ResponseEntity.ok(fiches.stream().map(this::toMap).toList());
    }

    // ── GET /api/fiches-techniques/{id} ───────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        FicheTechnique fiche = ficheRepo.findById(id).orElse(null);
        if (fiche == null) return ResponseEntity.notFound().build();

        boolean isTech = ROLES_TECH.contains(user.getRole());
        boolean isRssi = ROLES_RSSI.contains(user.getRole());

        if (isTech && !fiche.getActeur().getId().equals(user.getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
        if (!isTech && !isRssi)
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        return ResponseEntity.ok(toMap(fiche));
    }
}