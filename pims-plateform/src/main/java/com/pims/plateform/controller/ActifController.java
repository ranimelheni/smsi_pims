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
@RequestMapping("/api/actifs")
@RequiredArgsConstructor
public class ActifController {

    private final ActifRepository          actifRepository;
    private final UserRepository           userRepository;
    private final FicheProcessusRepository ficheProcessusRepository;
    private final FicheTechniqueRepository ficheTechniqueRepository;
    private final ObjectMapper             objectMapper = new ObjectMapper();

    private static final List<String> ROLES_RSSI = List.of("rssi", "super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    private boolean isRssi(User user) {
        return ROLES_RSSI.contains(user.getRole());
    }

    private int parseIntOrDefault(Object val, int def) {
        if (val == null) return def;
        try { return Integer.parseInt(val.toString()); }
        catch (Exception e) { return def; }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, List.class); }
        catch (Exception e) { return List.of(); }
    }

    private String mapRessourceType(String type) {
        return switch (type.toLowerCase()) {
            case "humain"    -> "humain";
            case "logiciel"  -> "logiciel";
            case "financier" -> "service";
            default          -> "materiel";
        };
    }

    private Map<String, Object> toMap(Actif a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",              a.getId());
        m.put("organism_id",     a.getOrganism().getId());
        m.put("nom",             a.getNom());
        m.put("code",            a.getCode());
        m.put("description",     a.getDescription());
        m.put("categorie",       a.getCategorie());
        m.put("confidentialite", a.getConfidentialite());
        m.put("integrite",       a.getIntegrite());
        m.put("disponibilite",   a.getDisponibilite());
        m.put("note_globale",    a.getNoteGlobale());
        m.put("niveau_critique", a.getNiveauCritique());
        m.put("proprietaire",    a.getProprietaire());
        m.put("gestionnaire",    a.getGestionnaire());
        m.put("localisation",    a.getLocalisation());
        m.put("source",          a.getSource());
        m.put("section_source",  a.getSectionSource());
        m.put("statut",          a.getStatut());
        m.put("commentaire_rejet", a.getCommentaireRejet());
        m.put("valide_by",       a.getValidePar() != null ? a.getValidePar().getId() : null);
        m.put("valide_at",       a.getValideAt()  != null ? a.getValideAt().toString() : null);
        m.put("created_at",      a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);
        return m;
    }

    // ── GET /api/actifs ───────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String statut,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!isRssi(user))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        Long orgId = user.getOrganism().getId();
        List<Actif> list;

        if (categorie != null)
            list = actifRepository.findByOrganismIdAndCategorie(orgId, categorie);
        else if (statut != null)
            list = actifRepository.findByOrganismIdAndStatut(orgId, statut);
        else
            list = actifRepository.findByOrganismIdOrderByCreatedAtDesc(orgId);

        return ResponseEntity.ok(list.stream().map(this::toMap).toList());
    }

    // ── GET /api/actifs/stats ─────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!isRssi(user))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        Long orgId = user.getOrganism().getId();
        List<Actif> all = actifRepository.findByOrganismIdOrderByCreatedAtDesc(orgId);

        return ResponseEntity.ok(Map.of(
            "total",              all.size(),
            "valides",            all.stream().filter(a -> "actif".equals(a.getStatut())).count(),
            "en_attente",         all.stream().filter(a -> "en_attente_validation".equals(a.getStatut())).count(),
            "par_categorie", Map.of(
                "materiel", all.stream().filter(a -> "materiel".equals(a.getCategorie())).count(),
                "logiciel",  all.stream().filter(a -> "logiciel".equals(a.getCategorie())).count(),
                "donnees",   all.stream().filter(a -> "donnees".equals(a.getCategorie())).count(),
                "service",   all.stream().filter(a -> "service".equals(a.getCategorie())).count(),
                "humain",    all.stream().filter(a -> "humain".equals(a.getCategorie())).count(),
                "site",      all.stream().filter(a -> "site".equals(a.getCategorie())).count()
            ),
            "par_criticite", Map.of(
                "secret",       all.stream().filter(a -> a.getNoteGlobale() == 4).count(),
                "confidentiel", all.stream().filter(a -> a.getNoteGlobale() == 3).count(),
                "interne",      all.stream().filter(a -> a.getNoteGlobale() == 2).count(),
                "public",       all.stream().filter(a -> a.getNoteGlobale() == 1).count()
            )
        ));
    }

    // ── GET /api/actifs/consolider ────────────────────────────────────────────
    @GetMapping("/consolider")
    public ResponseEntity<?> consolider(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!isRssi(user))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        Long orgId = user.getOrganism().getId();
        List<Map<String, Object>> candidates = new ArrayList<>();

        // ── Depuis fiches_processus ───────────────────────────────────────────
        ficheProcessusRepository.findByOrganismId(orgId).forEach(fiche -> {
            if (!List.of("soumis_rssi","complete_dpo","valide").contains(fiche.getStatut())) return;

            // Ressources → actifs
            parseJsonArray(fiche.getRessources()).forEach(r -> {
                String nom = r.getOrDefault("nom", "").toString();
                if (nom.isBlank()) return;
                candidates.add(buildCandidate(
                    nom,
                    r.getOrDefault("description", "").toString(),
                    mapRessourceType(r.getOrDefault("type","humain").toString()),
                    "fiche_processus", fiche.getId(), fiche.getIntitule(), "ressources",
                    2, 2, 2
                ));
            });

            // Informations documentées → données
            parseJsonArray(fiche.getInformationsDocumentees()).forEach(d -> {
                String nom = d.getOrDefault("titre", "").toString();
                if (nom.isBlank()) return;
                candidates.add(buildCandidate(
                    nom,
                    d.getOrDefault("description","").toString() + " [Réf: " + d.getOrDefault("reference","—") + "]",
                    "donnees",
                    "fiche_processus", fiche.getId(), fiche.getIntitule(), "informations_documentees",
                    2, 3, 2
                ));
            });
        });

        // ── Depuis fiches_techniques ──────────────────────────────────────────
        ficheTechniqueRepository.findByOrganismId(orgId).forEach(ft -> {
            if (!List.of("soumis_rssi","valide").contains(ft.getStatut())) return;

            addCandidatesFromTech(candidates, ft, ft.getActifsServeurs(),      "materiel", "serveurs");
            addCandidatesFromTech(candidates, ft, ft.getActifsPostes(),        "materiel", "postes");
            addCandidatesFromTech(candidates, ft, ft.getActifsReseau(),        "materiel", "reseau");
            addCandidatesFromTech(candidates, ft, ft.getActifsApplications(),  "logiciel", "applications");
            addCandidatesFromTech(candidates, ft, ft.getActifsLicences(),      "logiciel", "licences");
            addCandidatesFromTech(candidates, ft, ft.getActifsBdd(),           "donnees",  "bdd");
            addCandidatesFromTech(candidates, ft, ft.getActifsSauvegardes(),   "donnees",  "sauvegardes");
            addCandidatesFromTech(candidates, ft, ft.getActifsStockages(),     "donnees",  "stockages");
            addCandidatesFromTech(candidates, ft, ft.getActifsCloud(),         "service",  "cloud");
            addCandidatesFromTech(candidates, ft, ft.getActifsAcces(),         "service",  "acces");
            addCandidatesFromTech(candidates, ft, ft.getActifsCertificats(),   "service",  "certificats");
        });

        // Filtrer les doublons déjà en base
        List<Map<String, Object>> nouveaux = candidates.stream()
            .filter(c -> {
                String nom = c.getOrDefault("nom","").toString();
                String cat = c.getOrDefault("categorie","").toString();
                return !nom.isBlank() &&
                       !actifRepository.existsByOrganismIdAndNomAndCategorie(orgId, nom, cat);
            })
            .toList();

        return ResponseEntity.ok(Map.of(
            "total",    candidates.size(),
            "nouveaux", nouveaux.size(),
            "actifs",   nouveaux
        ));
    }

    private void addCandidatesFromTech(List<Map<String, Object>> candidates,
            FicheTechnique ft, String json, String categorie, String section) {
        parseJsonArray(json).forEach(item -> {
            String nom = item.getOrDefault("nom","").toString();
            if (nom.isBlank()) return;
            candidates.add(buildCandidate(
                nom,
                item.getOrDefault("description","").toString(),
                categorie,
                "fiche_technique", ft.getId(), ft.getIntitule(), section,
                parseIntOrDefault(item.get("confidentialite"), 2),
                parseIntOrDefault(item.get("integrite"),       2),
                parseIntOrDefault(item.get("disponibilite"),   2)
            ));
        });
    }

    private Map<String, Object> buildCandidate(String nom, String desc, String cat,
            String sourceType, Long sourceId, String sourceName, String section,
            int c, int i, int d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nom",             nom);
        m.put("description",     desc);
        m.put("categorie",       cat);
        m.put("source",          sourceType);
        m.put("source_id",       sourceId);
        m.put("source_name",     sourceName);
        m.put("section_source",  section);
        m.put("confidentialite", c);
        m.put("integrite",       i);
        m.put("disponibilite",   d);
        m.put("selected",        true);
        return m;
    }

    // ── POST /api/actifs/valider-lot ──────────────────────────────────────────
    @PostMapping("/valider-lot")
    public ResponseEntity<?> validerLot(@RequestBody List<Map<String, Object>> actifs,
                                        @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!isRssi(user))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        Organism org = user.getOrganism();
        int created = 0; int skipped = 0;

        for (Map<String, Object> item : actifs) {
            String nom      = item.getOrDefault("nom","").toString();
            String categorie = item.getOrDefault("categorie","materiel").toString();
            if (nom.isBlank()) continue;
            if (actifRepository.existsByOrganismIdAndNomAndCategorie(org.getId(), nom, categorie)) {
                skipped++; continue;
            }

            Actif actif = Actif.builder()
                    .organism(org)
                    .nom(nom)
                    .description(item.getOrDefault("description","").toString())
                    .categorie(categorie)
                    .confidentialite(parseIntOrDefault(item.get("confidentialite"), 2))
                    .integrite(parseIntOrDefault(item.get("integrite"),             2))
                    .disponibilite(parseIntOrDefault(item.get("disponibilite"),     2))
                    .source(item.getOrDefault("source","fiche_processus").toString())
                    .sectionSource(item.getOrDefault("section_source","").toString())
                    .statut("actif")
                    .validePar(user)
                    .valideAt(LocalDateTime.now())
                    .build();

            // Lier à la fiche source si possible
            Object srcId = item.get("source_id");
            if (srcId != null) {
                Long id = Long.parseLong(srcId.toString());
                if ("fiche_technique".equals(item.get("source"))) {
                    ficheTechniqueRepository.findById(id).ifPresent(actif::setFicheTechnique);
                } else {
                    ficheProcessusRepository.findById(id).ifPresent(actif::setFicheProcessus);
                }
            }

            actifRepository.save(actif);
            created++;
        }

        return ResponseEntity.status(201).body(Map.of(
            "created", created,
            "skipped", skipped,
            "message", created + " actifs validés, " + skipped + " doublons ignorés"
        ));
    }

    // ── POST /api/actifs (ajout manuel) ───────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!isRssi(user))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        if (data.get("nom") == null || data.get("categorie") == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Nom et catégorie requis"));

        Actif actif = Actif.builder()
                .organism(user.getOrganism())
                .nom(data.get("nom").toString())
                .code(data.containsKey("code")        ? data.get("code").toString()        : null)
                .description(data.containsKey("description") ? data.get("description").toString() : null)
                .categorie(data.get("categorie").toString())
                .confidentialite(parseIntOrDefault(data.get("confidentialite"), 2))
                .integrite(parseIntOrDefault(data.get("integrite"),             2))
                .disponibilite(parseIntOrDefault(data.get("disponibilite"),     2))
                .proprietaire(data.containsKey("proprietaire") ? data.get("proprietaire").toString() : null)
                .gestionnaire(data.containsKey("gestionnaire") ? data.get("gestionnaire").toString() : null)
                .localisation(data.containsKey("localisation") ? data.get("localisation").toString() : null)
                .source("manuel")
                .statut("actif")
                .validePar(user)
                .valideAt(LocalDateTime.now())
                .build();

        return ResponseEntity.status(201).body(toMap(actifRepository.save(actif)));
    }

    // ── PUT /api/actifs/{id} ──────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!isRssi(user))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        Actif actif = actifRepository.findById(id).orElse(null);
        if (actif == null) return ResponseEntity.notFound().build();

        if (data.containsKey("nom"))             actif.setNom(data.get("nom").toString());
        if (data.containsKey("code"))            actif.setCode(data.get("code").toString());
        if (data.containsKey("description"))     actif.setDescription(data.get("description").toString());
        if (data.containsKey("categorie"))       actif.setCategorie(data.get("categorie").toString());
        if (data.containsKey("confidentialite")) actif.setConfidentialite(parseIntOrDefault(data.get("confidentialite"), 2));
        if (data.containsKey("integrite"))       actif.setIntegrite(parseIntOrDefault(data.get("integrite"), 2));
        if (data.containsKey("disponibilite"))   actif.setDisponibilite(parseIntOrDefault(data.get("disponibilite"), 2));
        if (data.containsKey("proprietaire"))    actif.setProprietaire(data.get("proprietaire").toString());
        if (data.containsKey("gestionnaire"))    actif.setGestionnaire(data.get("gestionnaire").toString());
        if (data.containsKey("localisation"))    actif.setLocalisation(data.get("localisation").toString());
        if (data.containsKey("statut"))          actif.setStatut(data.get("statut").toString());

        return ResponseEntity.ok(toMap(actifRepository.save(actif)));
    }

    // ── DELETE /api/actifs/{id} ───────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!isRssi(user))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        Actif actif = actifRepository.findById(id).orElse(null);
        if (actif == null) return ResponseEntity.notFound().build();

        actif.setStatut("inactif");
        actifRepository.save(actif);
        return ResponseEntity.ok(Map.of("message", "Actif désactivé"));
    }
}