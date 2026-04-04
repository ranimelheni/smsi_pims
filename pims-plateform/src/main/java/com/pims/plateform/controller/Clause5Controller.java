package com.pims.plateform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.entity.*;
import com.pims.plateform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/clause5")
@RequiredArgsConstructor
public class Clause5Controller {

    private final Clause5Repository        clause5Repository;
    private final OrganigrammeRepository   organigrammeRepository;
    private final SensibilisationRepository sensibilisationRepository;
    private final OrganismRepository       organismRepository;
    private final UserRepository           userRepository;
    private final ObjectMapper             objectMapper = new ObjectMapper();

    private static final List<String> ROLES_DIRECTION = List.of("direction", "super_admin");

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

    private Map<String, Object> toMap(Clause5 c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          c.getId());
        m.put("organism_id", c.getOrganism().getId());

        // Validations
        m.put("validation_enjeux_externes",  c.getValidationEnjeuxExternes());
        m.put("validation_enjeux_internes",  c.getValidationEnjeuxInternes());
        m.put("validation_parties",          c.getValidationParties());
        m.put("validation_perimetre",        c.getValidationPerimetre());
        m.put("validation_ressources",       c.getValidationRessources());
        m.put("commentaire_enjeux_externes", c.getCommentaireEnjeuxExternes());
        m.put("commentaire_enjeux_internes", c.getCommentaireEnjeuxInternes());
        m.put("commentaire_parties",         c.getCommentaireParties());
        m.put("commentaire_perimetre",       c.getCommentairePerimetre());
        m.put("commentaire_ressources",      c.getCommentaireRessources());

        // Politique
        m.put("politique_securite_contenu",  c.getPolitiqueSecuriteContenu());
        m.put("politique_diffusion",         fromJson(c.getPolitiqueDiffusion()));
        m.put("objectifs_securite_metier",   fromJson(c.getObjectifsSecuriteMetier()));

        // Rôles
        m.put("exigences_processus",         fromJson(c.getExigencesProcessus()));
        m.put("ressources_smsi",             fromJson(c.getRessourcesSmsi()));

        // Indicateurs
        m.put("indicateurs_smsi",            fromJson(c.getIndicateursSmsi()));
        m.put("statut",                      c.getStatut());
        m.put("updated_at", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
        return m;
    }

    // ── GET /api/clause5 ──────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null) return ResponseEntity.ok(Map.of());

        Clause5 c = clause5Repository.findByOrganismId(orgId).orElse(null);
        if (c == null) return ResponseEntity.ok(Map.of());
        return ResponseEntity.ok(toMap(c));
    }

    // ── POST /api/clause5 (direction saisit/met à jour) ───────────────────────
    @PostMapping
    public ResponseEntity<?> save(@RequestBody Map<String, Object> data,
                                  @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_DIRECTION.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à la direction"));

        Long orgId = user.getOrganism().getId();
        Organism org = organismRepository.findById(orgId).orElseThrow();
        Clause5 c = clause5Repository.findByOrganismId(orgId)
                .orElse(Clause5.builder().organism(org).build());

        // Politique
        if (data.containsKey("politique_securite_contenu"))
            c.setPolitiqueSecuriteContenu(str(data, "politique_securite_contenu"));
        if (data.containsKey("politique_diffusion"))
            c.setPolitiqueDiffusion(toJson(data.get("politique_diffusion")));
        if (data.containsKey("objectifs_securite_metier"))
            c.setObjectifsSecuriteMetier(toJson(data.get("objectifs_securite_metier")));

        // Rôles
        if (data.containsKey("exigences_processus"))
            c.setExigencesProcessus(toJson(data.get("exigences_processus")));
        if (data.containsKey("ressources_smsi"))
            c.setRessourcesSmsi(toJson(data.get("ressources_smsi")));

        // Indicateurs
        if (data.containsKey("indicateurs_smsi"))
            c.setIndicateursSmsi(toJson(data.get("indicateurs_smsi")));

        return ResponseEntity.ok(toMap(clause5Repository.save(c)));
    }

    // ── PUT /api/clause5/valider-section ──────────────────────────────────────
    // Direction valide section par section
    @PutMapping("/valider-section")
    public ResponseEntity<?> validerSection(@RequestBody Map<String, Object> data,
                                             @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_DIRECTION.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à la direction"));

        Long orgId = user.getOrganism().getId();
        Organism org = organismRepository.findById(orgId).orElseThrow();
        Clause5 c = clause5Repository.findByOrganismId(orgId)
                .orElse(Clause5.builder().organism(org).build());

        String section = str(data, "section");
        String decision = str(data, "decision"); // approuve | rejete
        String commentaire = str(data, "commentaire");

        if (decision == null || !List.of("approuve","rejete","en_attente").contains(decision))
            return ResponseEntity.badRequest().body(Map.of("error", "Décision invalide"));

        switch (section != null ? section : "") {
            case "enjeux_externes" -> { c.setValidationEnjeuxExternes(decision); c.setCommentaireEnjeuxExternes(commentaire); }
            case "enjeux_internes" -> { c.setValidationEnjeuxInternes(decision); c.setCommentaireEnjeuxInternes(commentaire); }
            case "parties"         -> { c.setValidationParties(decision);        c.setCommentaireParties(commentaire); }
            case "perimetre"       -> { c.setValidationPerimetre(decision);      c.setCommentairePerimetre(commentaire); }
            case "ressources"      -> { c.setValidationRessources(decision);     c.setCommentaireRessources(commentaire); }
            default -> { return ResponseEntity.badRequest().body(Map.of("error", "Section inconnue: " + section)); }
        }

        // Vérifier si tout est approuvé
        Clause5 saved = clause5Repository.save(c);
        boolean toutApprouve = List.of(
            saved.getValidationEnjeuxExternes(), saved.getValidationEnjeuxInternes(),
            saved.getValidationParties(), saved.getValidationPerimetre(),
            saved.getValidationRessources()
        ).stream().allMatch("approuve"::equals);

        if (toutApprouve) { saved.setStatut("valide"); clause5Repository.save(saved); }

        return ResponseEntity.ok(Map.of(
            "section",   section,
            "decision",  decision,
            "statut_global", saved.getStatut(),
            "message",   "Section " + section + " : " + decision
        ));
    }

    // ── POST /api/clause5/organigramme ────────────────────────────────────────
    @PostMapping("/organigramme")
    public ResponseEntity<?> uploadOrganigramme(
            @RequestParam("fichier") MultipartFile fichier,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_DIRECTION.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à la direction"));

        if (fichier.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));

        String contentType = fichier.getContentType();
        if (contentType == null || (!contentType.contains("pdf") && !contentType.startsWith("image/")))
            return ResponseEntity.badRequest().body(Map.of("error", "Format non supporté (PDF ou image requis)"));

        try {
            Organism org = user.getOrganism();

            // Désactiver l'ancien organigramme
            organigrammeRepository.findByOrganismIdAndIsActiveTrue(org.getId())
                    .forEach(o -> { o.setIsActive(false); organigrammeRepository.save(o); });

            Organigramme orga = Organigramme.builder()
                    .organism(org)
                    .nomFichier(fichier.getOriginalFilename())
                    .typeFichier(contentType.contains("pdf") ? "pdf" : "image")
                    .contenu(fichier.getBytes())
                    .taille(fichier.getSize())
                    .uploadedBy(user)
                    .isActive(true)
                    .build();

            Organigramme saved = organigrammeRepository.save(orga);

            return ResponseEntity.status(201).body(Map.of(
                "id",          saved.getId(),
                "nom_fichier", saved.getNomFichier(),
                "type",        saved.getTypeFichier(),
                "taille",      saved.getTaille(),
                "uploaded_at", saved.getUploadedAt().toString(),
                "message",     "Organigramme uploadé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur upload: " + e.getMessage()));
        }
    }

    // ── GET /api/clause5/organigramme ─────────────────────────────────────────
    @GetMapping("/organigramme")
    public ResponseEntity<?> getOrganigramme(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null) return ResponseEntity.notFound().build();

        return organigrammeRepository
                .findFirstByOrganismIdAndIsActiveTrueOrderByUploadedAtDesc(orgId)
                .map(o -> ResponseEntity.ok(Map.of(
                    "id",          o.getId(),
                    "nom_fichier", o.getNomFichier(),
                    "type",        o.getTypeFichier(),
                    "taille",      o.getTaille(),
                    "uploaded_at", o.getUploadedAt().toString(),
                    "uploaded_by", o.getUploadedBy() != null
                        ? o.getUploadedBy().getPrenom() + " " + o.getUploadedBy().getNom() : "—"
                )))
                .orElse(ResponseEntity.ok(Map.of()));
    }

    // ── GET /api/clause5/organigramme/download ────────────────────────────────
    @GetMapping("/organigramme/download")
    public ResponseEntity<byte[]> downloadOrganigramme(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null) return ResponseEntity.notFound().build();

        return organigrammeRepository
                .findFirstByOrganismIdAndIsActiveTrueOrderByUploadedAtDesc(orgId)
                .map(o -> {
                    MediaType mt = "pdf".equals(o.getTypeFichier())
                        ? MediaType.APPLICATION_PDF
                        : MediaType.IMAGE_PNG;
                    return ResponseEntity.ok()
                            .contentType(mt)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + o.getNomFichier() + "\"")
                            .body(o.getContenu());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ── GET /api/clause5/sensibilisation ──────────────────────────────────────
    @GetMapping("/sensibilisation")
    public ResponseEntity<?> getSensibilisation(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null) return ResponseEntity.ok(List.of());

        List<Sensibilisation> list = sensibilisationRepository
                .findByOrganismIdOrderByDateEcheanceAsc(orgId);

        long realise   = sensibilisationRepository.countByOrganismIdAndStatut(orgId, "realise");
        long planifie  = sensibilisationRepository.countByOrganismIdAndStatut(orgId, "planifie");
        long enCours   = sensibilisationRepository.countByOrganismIdAndStatut(orgId, "en_cours");
        long nonRealise= sensibilisationRepository.countByOrganismIdAndStatut(orgId, "non_realise");

        return ResponseEntity.ok(Map.of(
            "items", list.stream().map(this::sensToMap).toList(),
            "stats", Map.of(
                "total",      list.size(),
                "realise",    realise,
                "planifie",   planifie,
                "en_cours",   enCours,
                "non_realise",nonRealise,
                "taux", list.isEmpty() ? 0 : (realise * 100 / list.size())
            )
        ));
    }

    // ── POST /api/clause5/sensibilisation ─────────────────────────────────────
    @PostMapping("/sensibilisation")
    public ResponseEntity<?> createSensibilisation(
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_DIRECTION.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à la direction"));

        Long orgId   = user.getOrganism().getId();
        Long acteurId = Long.parseLong(data.get("acteur_id").toString());

        User acteur = userRepository.findById(acteurId).orElse(null);
        if (acteur == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Acteur non trouvé"));

        Organism org = organismRepository.findById(orgId).orElseThrow();

        Sensibilisation s = Sensibilisation.builder()
                .organism(org)
                .acteur(acteur)
                .titre(str(data, "titre"))
                .type(data.getOrDefault("type","formation").toString())
                .statut(data.getOrDefault("statut","planifie").toString())
                .commentaire(str(data, "commentaire"))
                .createdBy(user)
                .build();

        if (data.get("date_realisation") != null)
            s.setDateRealisation(java.time.LocalDate.parse(str(data, "date_realisation")));
        if (data.get("date_echeance") != null)
            s.setDateEcheance(java.time.LocalDate.parse(str(data, "date_echeance")));
        if (data.get("score") != null)
            s.setScore(Integer.parseInt(data.get("score").toString()));

        return ResponseEntity.status(201).body(sensToMap(sensibilisationRepository.save(s)));
    }

    // ── PUT /api/clause5/sensibilisation/{id} ─────────────────────────────────
    @PutMapping("/sensibilisation/{id}")
    public ResponseEntity<?> updateSensibilisation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_DIRECTION.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à la direction"));

        Sensibilisation s = sensibilisationRepository.findById(id).orElse(null);
        if (s == null) return ResponseEntity.notFound().build();

        if (data.containsKey("statut"))       s.setStatut(str(data, "statut"));
        if (data.containsKey("score"))        s.setScore(Integer.parseInt(data.get("score").toString()));
        if (data.containsKey("commentaire"))  s.setCommentaire(str(data, "commentaire"));
        if (data.containsKey("date_realisation") && data.get("date_realisation") != null)
            s.setDateRealisation(java.time.LocalDate.parse(str(data, "date_realisation")));

        return ResponseEntity.ok(sensToMap(sensibilisationRepository.save(s)));
    }

    // ── DELETE /api/clause5/sensibilisation/{id} ──────────────────────────────
    @DeleteMapping("/sensibilisation/{id}")
    public ResponseEntity<?> deleteSensibilisation(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_DIRECTION.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à la direction"));

        sensibilisationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Supprimé"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Map<String, Object> sensToMap(Sensibilisation s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               s.getId());
        m.put("acteur_id",        s.getActeur().getId());
        m.put("acteur_nom",       s.getActeur().getPrenom() + " " + s.getActeur().getNom());
        m.put("acteur_role",      s.getActeur().getRole());
        m.put("titre",            s.getTitre());
        m.put("type",             s.getType());
        m.put("statut",           s.getStatut());
        m.put("score",            s.getScore());
        m.put("commentaire",      s.getCommentaire());
        m.put("date_realisation", s.getDateRealisation() != null ? s.getDateRealisation().toString() : null);
        m.put("date_echeance",    s.getDateEcheance()    != null ? s.getDateEcheance().toString()    : null);
        m.put("updated_at",       s.getUpdatedAt()       != null ? s.getUpdatedAt().toString()       : null);
        return m;
    }

    private String str(Map<String, Object> data, String key) {
        return data.get(key) != null ? data.get(key).toString() : null;
    }
}