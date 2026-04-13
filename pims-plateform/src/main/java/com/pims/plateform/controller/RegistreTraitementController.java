package com.pims.plateform.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.entity.FicheProcessus;
import com.pims.plateform.entity.RegistreTraitement;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.FicheProcessusRepository;
import com.pims.plateform.repository.RegistreTraitementRepository;
import com.pims.plateform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/registre-traitement")
@RequiredArgsConstructor
public class RegistreTraitementController {

    private final RegistreTraitementRepository registreRepo;
    private final FicheProcessusRepository     ficheRepo;
    private final UserRepository               userRepo;
    private final ObjectMapper                 om;

    private static final List<String> ROLES_ALLOWED =
            List.of("dpo", "rssi", "super_admin", "admin_organism");

    // ── Auth ──────────────────────────────────────────────────────────────
    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername()))
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    // ── JSON helpers ──────────────────────────────────────────────────────
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

    private Map<String, Object> parseDataDpo(FicheProcessus fiche) {
        if (fiche.getDataDpo() == null || fiche.getDataDpo().isBlank()
                || "{}".equals(fiche.getDataDpo().trim()))
            return new HashMap<>();
        try {
            return om.readValue(fiche.getDataDpo(), new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Impossible de parser dataDpo pour fiche #{}: {}", fiche.getId(), e.getMessage());
            return new HashMap<>();
        }
    }

    private String extractStr(Map<String, Object> data, String key) {
        return data.get(key) != null ? data.get(key).toString() : null;
    }

    private Boolean extractBool(Map<String, Object> data, String key) {
        Object v = data.get(key);
        if (v == null) return false;
        return Boolean.parseBoolean(v.toString());
    }

    private Short toShort(Object v, short def) {
        if (v == null) return def;
        try { return Short.parseShort(v.toString()); }
        catch (Exception e) { return def; }
    }

    // ── Calcul risque ─────────────────────────────────────────────────────
    private void calculerRisque(RegistreTraitement rt) {
        short p  = rt.getRisquePhysique()  != null ? rt.getRisquePhysique()  : 0;
        short m  = rt.getRisqueMoral()     != null ? rt.getRisqueMoral()     : 0;
        short ma = rt.getRisqueMateriel()  != null ? rt.getRisqueMateriel()  : 0;
        short max = (short) Math.max(p, Math.max(m, ma));
        rt.setNoteMax(max);

        if (max >= 3)      { rt.setRisqueMax("Élevé");  rt.setPiaRequis(true);  }
        else if (max == 2) { rt.setRisqueMax("Moyen");  rt.setPiaRequis(false); }
        else if (max == 1) { rt.setRisqueMax("Faible"); rt.setPiaRequis(false); }
        else               { rt.setRisqueMax("Aucun");  rt.setPiaRequis(false); }

        if (p >= m && p >= ma)      rt.setDomaineRisqueMax("Physique");
        else if (m >= p && m >= ma) rt.setDomaineRisqueMax("Moral");
        else                        rt.setDomaineRisqueMax("Matériel");
    }

    // ── Mapper entité → Map ───────────────────────────────────────────────
   private Map<String, Object> toMap(RegistreTraitement r) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id",                            r.getId());
    m.put("service",                       r.getService());
    m.put("macro_finalite",                r.getMacroFinalite());
    m.put("micro_finalites",               r.getMicroFinalites());
    m.put("autres_finalites",              r.getAutresFinalites());
    m.put("categorie_traitement",          r.getCategorieTraitement());
    m.put("date_creation",                 r.getDateCreation()    != null ? r.getDateCreation().toString()    : null);
    m.put("date_mise_a_jour",              r.getDateMiseAJour()   != null ? r.getDateMiseAJour().toString()   : null);
    m.put("base_legale",                   r.getBaseLegale());
    m.put("categories_personnes",          fromJson(r.getCategoriesPersonnes()));
    m.put("nombre_personnes",              r.getNombrePersonnes());
    m.put("categories_donnees",            fromJson(r.getCategoriesDonnees()));
    m.put("details_donnees",               r.getDetailsDonnees());
    m.put("donnees_hautement_personnelles",r.getDonneesHautementPersonnelles());
    m.put("donnees_sensibles",             fromJson(r.getDonneesSensibles()));
    m.put("mode_collecte",                 r.getModeCollecte());
    m.put("duree_conservation_definie",    r.getDureeConservationDefinie());
    m.put("duree_conservation",            r.getDureeConservation());
    m.put("information_personnes",         r.getInformationPersonnes());
    m.put("mode_consentement",             r.getModeConsentement());
    m.put("droit_acces",                   r.getDroitAcces());
    m.put("droit_rectification",           r.getDroitRectification());
    m.put("droit_opposition",              r.getDroitOpposition());
    m.put("droit_portabilite",             r.getDroitPortabilite());
    m.put("droit_limitation",              r.getDroitLimitation());
    m.put("notification_violation",        r.getNotificationViolation());
    m.put("gestion_conservation",          r.getGestionConservation());
    m.put("acces_donnees",                 r.getAccesDonnees());
    m.put("partage_donnees",               r.getPartageDonnees());
    m.put("type_destinataires",            fromJson(r.getTypeDestinataires()));
    m.put("prestataire",                   r.getPrestataire());
    m.put("contrat_prestataire",           r.getContratPrestataire());
    m.put("clause_protection_donnees",     r.getClauseProtectionDonnees());
    m.put("transfert_hors_ue",             r.getTransfertHorsUE());
    m.put("pays_destination",              r.getPaysDestination());
    m.put("fondement_transfert",           r.getFondementTransfert());
    m.put("outil_utilise",                 r.getOutilUtilise());
    m.put("description_outil",             r.getDescriptionOutil());
    m.put("methode_stockage",              r.getMethodeStockage());
    m.put("lieu_stockage",                 r.getLieuStockage());
    m.put("pays_stockage",                 r.getPaysStockage());
    m.put("securite_physique",             r.getSecuritePhysique());
    m.put("authentification",              r.getAuthentification());
    m.put("journalisation",                r.getJournalisation());
    m.put("reseau_interne",                r.getReseauInterne());
    m.put("chiffrement",                   r.getChiffrement());
    m.put("autres_mesures_securite",       r.getAutresMesuresSecurite());
    m.put("mesures_a_implementer",         r.getMesuresAImplementer());
    m.put("responsable_traitement",        r.getResponsableTraitement());
    m.put("responsable_conjoint",          r.getResponsableConjoint());
    m.put("contact_interne",               r.getContactInterne());
    m.put("region_responsable",            r.getRegionResponsable());
    m.put("reference_cnil",                r.getReferenceCnil());
    m.put("risque_physique",               r.getRisquePhysique());
    m.put("risque_moral",                  r.getRisqueMoral());
    m.put("risque_materiel",               r.getRisqueMateriel());
    m.put("note_max",                      r.getNoteMax());
    m.put("risque_max",                    r.getRisqueMax());
    m.put("domaine_risque_max",            r.getDomaineRisqueMax());
    m.put("explication_risque",            r.getExplicationRisque());
    m.put("analyse_pia",                   r.getAnalysePia());
    m.put("pia_requis",                    r.getPiaRequis());
    m.put("pia_statut",                    r.getPiaStatut());
    m.put("statut",                        r.getStatut());
    m.put("fiche_processus_id",
            r.getFicheProcessus() != null ? r.getFicheProcessus().getId() : null);

    // ← Sécurisé : ne pas appeler getPrenom() sur un proxy non initialisé
    String createdBy = "—";
    try {
        if (r.getCreatedBy() != null) {
            createdBy = r.getCreatedBy().getPrenom() + " " + r.getCreatedBy().getNom();
        }
    } catch (Exception e) {
        log.warn("createdBy non chargé pour registre #{}", r.getId());
    }
    m.put("created_by", createdBy);
    m.put("created_at", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
    m.put("updated_at", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);
    return m;
}

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/registre-traitement
    // Synchronise auto les fiches validées puis retourne le registre complet
    // ════════════════════════════════════════════════════════════════════════
    @GetMapping
    @Transactional  // ← AJOUT : garde la session ouverte pour tout le traitement
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism().getId();

        // 1. Auto-sync : importer toute fiche "valide" pas encore dans le registre
        syncFichesValidees(user, orgId);

        // 2. Charger le registre
        List<RegistreTraitement> list =
                registreRepo.findByOrganismIdOrderByServiceAsc(orgId);

        long total   = list.size();
        long valides = registreRepo.countByOrganismIdAndStatut(orgId, "valide");
        long piaReq  = registreRepo.countByOrganismIdAndPiaRequis(orgId, true);
        long piaFait = registreRepo.countByOrganismIdAndAnalysePia(orgId, true);

        return ResponseEntity.ok(Map.of(
            "registres", list.stream().map(this::toMap).toList(),
            "stats", Map.of(
                "total",        total,
                "valides",      valides,
                "brouillons",   total - valides,
                "pia_requis",   piaReq,
                "pia_realise",  piaFait,
                "pia_manquant", Math.max(0, piaReq - piaFait)
            )
        ));
    }

    // ── Synchronisation automatique ───────────────────────────────────────
    private void syncFichesValidees(User user, Long orgId) {
        // Récupérer toutes les fiches validées par le RSSI
        List<FicheProcessus> fichesValidees =
                ficheRepo.findByOrganismIdAndStatut(orgId, "valide");

        // IDs déjà dans le registre
        Set<Long> dejaDansRegistre = registreRepo
                .findByOrganismIdWithFiche(orgId)
                .stream()
                .map(r -> r.getFicheProcessus().getId())
                .collect(Collectors.toSet());

        for (FicheProcessus fiche : fichesValidees) {
            if (dejaDansRegistre.contains(fiche.getId())) continue;
            try {
                RegistreTraitement rt = buildFromFiche(fiche, user);
                calculerRisque(rt);
                registreRepo.save(rt);
                log.info("Auto-import fiche #{} → registre", fiche.getId());
            } catch (Exception e) {
                log.error("Erreur auto-import fiche #{}: {}", fiche.getId(), e.getMessage());
            }
        }
    }

    // ── Construire un RegistreTraitement depuis une FicheProcessus ────────
    private RegistreTraitement buildFromFiche(FicheProcessus fiche, User user) {
        Map<String, Object> dpo = parseDataDpo(fiche);

        return RegistreTraitement.builder()
            .organism(user.getOrganism())
            .createdBy(user)
            .ficheProcessus(fiche)
            // Identification
            .service(fiche.getIntitule() != null ? fiche.getIntitule() : "Service #" + fiche.getId())
            .macroFinalite(fiche.getFinalite())
            .microFinalites(extractStr(dpo, "micro_finalites"))
            .categorieTraitement(extractStr(dpo, "categorie_traitement"))
            // Base légale & responsabilités
            .baseLegale(extractStr(dpo, "base_legale"))
            .responsableTraitement(extractStr(dpo, "responsable_nom"))
            .contactInterne(extractStr(dpo, "contact_interne"))
            .regionResponsable(extractStr(dpo, "region_responsable"))
            // Personnes
            .nombrePersonnes(extractStr(dpo, "nombre_personnes"))
            .categoriesPersonnes(toJson(dpo.getOrDefault("categories_personnes", List.of())))
            // Données
            .categoriesDonnees(toJson(dpo.getOrDefault("categories_donnees", List.of())))
            .donneesSensibles(toJson(dpo.getOrDefault("donnees_sensibles", List.of())))
            .donneesHautementPersonnelles(extractBool(dpo, "donnees_hautement_personnelles"))
            // Collecte & conservation
            .modeCollecte(extractStr(dpo, "mode_collecte"))
            .dureeConservation(extractStr(dpo, "duree_conservation"))
            .dureeConservationDefinie(extractBool(dpo, "duree_conservation_definie"))
            // Droits
            .droitAcces(extractBool(dpo, "droit_acces"))
            .droitRectification(extractBool(dpo, "droit_rectification"))
            .droitOpposition(extractBool(dpo, "droit_opposition"))
            .droitPortabilite(extractBool(dpo, "droit_portabilite"))
            .droitLimitation(extractBool(dpo, "droit_limitation"))
            .notificationViolation(extractBool(dpo, "notification_violation"))
            .informationPersonnes(extractStr(dpo, "information_personnes"))
            .modeConsentement(extractStr(dpo, "mode_consentement"))
            // Partage
            .partageDonnees(extractBool(dpo, "partage_donnees"))
            .typeDestinataires(toJson(dpo.getOrDefault("type_destinataires", List.of())))
            .prestataire(extractStr(dpo, "prestataire"))
            .transfertHorsUE(extractBool(dpo, "transfert_hors_ue"))
            .paysDestination(extractStr(dpo, "pays_destination"))
            .fondementTransfert(extractStr(dpo, "fondement_transfert"))
            // Outils & stockage
            .outilUtilise(extractStr(dpo, "outil_utilise"))
            .lieuStockage(extractStr(dpo, "lieu_stockage"))
            .paysStockage(extractStr(dpo, "pays_stockage"))
            // Sécurité
            .authentification(extractBool(dpo, "authentification"))
            .journalisation(extractBool(dpo, "journalisation"))
            .chiffrement(extractBool(dpo, "chiffrement"))
            .reseauInterne(extractBool(dpo, "reseau_interne"))
            // Risques depuis fiche processus
            .risquePhysique(toShort(fiche.getNoteMax(), (short) 0))
            .risqueMoral((short) 0)
            .risqueMateriel((short) 0)
            // Dates & statut
            .dateCreation(LocalDate.now())
            .statut("brouillon")
            .piaStatut("non_requis")
            .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // GET /api/registre-traitement/{id}
    // ════════════════════════════════════════════════════════════════════════
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        RegistreTraitement r = registreRepo.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toMap(r));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PUT /api/registre-traitement/{id}  — mise à jour partielle
    // ════════════════════════════════════════════════════════════════════════
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_ALLOWED.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        RegistreTraitement rt = registreRepo.findById(id).orElse(null);
        if (rt == null) return ResponseEntity.notFound().build();

        applyPatch(rt, d);
        calculerRisque(rt);
        return ResponseEntity.ok(toMap(registreRepo.save(rt)));
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATCH /api/registre-traitement/{id}/pia  — mise à jour statut PIA seul
    // ════════════════════════════════════════════════════════════════════════
    @PatchMapping("/{id}/pia")
    public ResponseEntity<?> updatePia(
            @PathVariable Long id,
            @RequestBody Map<String, Object> d,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!ROLES_ALLOWED.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

        RegistreTraitement rt = registreRepo.findById(id).orElse(null);
        if (rt == null) return ResponseEntity.notFound().build();

        if (d.containsKey("analyse_pia"))
            rt.setAnalysePia(Boolean.parseBoolean(d.get("analyse_pia").toString()));
        if (d.containsKey("pia_statut"))
            rt.setPiaStatut(d.get("pia_statut").toString());

        return ResponseEntity.ok(toMap(registreRepo.save(rt)));
    }

    // ════════════════════════════════════════════════════════════════════════
    // DELETE /api/registre-traitement/{id}
    // ════════════════════════════════════════════════════════════════════════
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_ALLOWED.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
        registreRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Supprimé"));
    }

    // ── Patch helper ──────────────────────────────────────────────────────
    private void applyPatch(RegistreTraitement rt, Map<String, Object> d) {
        if (d.containsKey("statut"))               rt.setStatut(d.get("statut").toString());
        if (d.containsKey("risque_physique"))       rt.setRisquePhysique(toShort(d.get("risque_physique"), (short)0));
        if (d.containsKey("risque_moral"))          rt.setRisqueMoral(toShort(d.get("risque_moral"), (short)0));
        if (d.containsKey("risque_materiel"))       rt.setRisqueMateriel(toShort(d.get("risque_materiel"), (short)0));
        if (d.containsKey("explication_risque"))    rt.setExplicationRisque(d.get("explication_risque").toString());
        if (d.containsKey("analyse_pia"))           rt.setAnalysePia(Boolean.parseBoolean(d.get("analyse_pia").toString()));
        if (d.containsKey("pia_statut"))            rt.setPiaStatut(d.get("pia_statut").toString());
        if (d.containsKey("mesures_a_implementer")) rt.setMesuresAImplementer(d.get("mesures_a_implementer").toString());
        if (d.containsKey("date_mise_a_jour") && d.get("date_mise_a_jour") != null)
            rt.setDateMiseAJour(LocalDate.parse(d.get("date_mise_a_jour").toString()));
    }
}