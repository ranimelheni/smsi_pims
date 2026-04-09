package com.pims.plateform.controller;

import com.pims.plateform.entity.*;
import com.pims.plateform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/soa")
@RequiredArgsConstructor
public class SoaController {

    private final SoaRepository         soaRepository;
    private final SoaControleRepository controleRepository;
    private final OrganismRepository    organismRepository;
    private final UserRepository        userRepository;

    private static final List<String> ROLES_RSSI      = List.of("rssi", "super_admin");
    private static final List<String> ROLES_DIRECTION  = List.of("direction", "super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    private Map<String, Object> controleToMap(SoaControle c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                     c.getId());
        m.put("annexe",                 c.getAnnexe());
        m.put("annexe_label",           c.getAnnexeLabel());
        m.put("controle_id",            c.getControleId());
        m.put("controle_label",         c.getControleLabel());
        m.put("description",            c.getDescription());
        m.put("norme",                  c.getNorme());
        m.put("inclus",                 c.getInclus());
        m.put("justification_exclusion",c.getJustificationExclusion());
        m.put("reference_doc",          c.getReferenceDoc());
        m.put("statut_impl",            c.getStatutImpl());
        m.put("responsable",            c.getResponsable());
        m.put("echeance",               c.getEcheance() != null ? c.getEcheance().toString() : null);
        return m;
    }

    private Map<String, Object> soaToMap(Soa soa, List<SoaControle> controles) {
        long total     = controles.size();
        long inclus    = controles.stream().filter(SoaControle::getInclus).count();
        long exclus    = total - inclus;
        long implemente = controles.stream()
            .filter(c -> c.getInclus() && "implemente".equals(c.getStatutImpl())).count();

        // Grouper par annexe
        Map<String, List<Map<String, Object>>> parAnnexe = new LinkedHashMap<>();
        controles.forEach(c ->
            parAnnexe.computeIfAbsent(c.getAnnexe(), k -> new ArrayList<>())
                     .add(controleToMap(c))
        );

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id",                    soa.getId());
        r.put("audit_type",            soa.getAuditType());
        r.put("role_organisme",        soa.getRoleOrganisme());
        r.put("version",               soa.getVersion());
        r.put("statut",                soa.getStatut());
        r.put("commentaire_direction", soa.getCommentaireDirection());
        r.put("valide_at",             soa.getValideAt() != null ? soa.getValideAt().toString() : null);
        r.put("stats", Map.of(
            "total",      total,
            "inclus",     inclus,
            "exclus",     exclus,
            "implemente", implemente,
            "taux_impl",  inclus > 0 ? (implemente * 100 / inclus) : 0
        ));
        r.put("par_annexe", parAnnexe);
        r.put("controles",  controles.stream().map(this::controleToMap).toList());
        return r;
    }

    // ── GET /api/soa ──────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<?> get(@AuthenticationPrincipal UserDetails ud) {
        User user  = getCurrentUser(ud);
        Long orgId = user.getOrganism() != null ? user.getOrganism().getId() : null;
        if (orgId == null) return ResponseEntity.ok(Map.of());

        Soa soa = soaRepository.findByOrganismId(orgId).orElse(null);
        if (soa == null) return ResponseEntity.ok(Map.of());

        List<SoaControle> controles = controleRepository.findBySoa_IdOrderByControleId(soa.getId());
        return ResponseEntity.ok(soaToMap(soa, controles));
    }

    // ── POST /api/soa/init ────────────────────────────────────────────────────
    @PostMapping("/init")
    public ResponseEntity<?> init(@RequestBody(required = false) Map<String, Object> body,
                                  @AuthenticationPrincipal UserDetails ud) {
        User user  = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Long orgId = user.getOrganism().getId();
        Organism org = organismRepository.findById(orgId).orElseThrow();

        if (soaRepository.findByOrganismId(orgId).isPresent())
            return ResponseEntity.badRequest().body(Map.of("error", "SoA déjà initialisée"));

        // Détecter audit_type et role depuis l'organisme + clause5
String auditType = org.getAuditType() != null ? org.getAuditType().name().toLowerCase().replace("_", "") : "iso27001";
       
        String role = body != null && body.get("role_organisme") != null
            ? body.get("role_organisme").toString() : "responsable";

        Soa soa = soaRepository.save(Soa.builder()
                .organism(org)
                .auditType(auditType)
                .roleOrganisme(role)
                .build());

        List<SoaControle> controles;
        if ("iso27701".equals(auditType)) {
            controles = buildControlesIso27701(soa, org, role);
        } else {
            controles = buildControlesIso27001(soa, org);
        }

        controleRepository.saveAll(controles);

        return ResponseEntity.status(201).body(Map.of(
            "message",  "SoA " + auditType.toUpperCase() + " initialisée avec " + controles.size() + " contrôles",
            "soa_id",   soa.getId(),
            "audit_type", auditType
        ));
    }

    // ── PUT /api/soa/controles/batch ──────────────────────────────────────────
    // Sauvegarder plusieurs contrôles en une fois
    @PutMapping("/controles/batch")
    public ResponseEntity<?> updateBatch(@RequestBody List<Map<String, Object>> updates,
                                         @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        int updated = 0;
        for (Map<String, Object> data : updates) {
            Long id = Long.parseLong(data.get("id").toString());
            SoaControle c = controleRepository.findById(id).orElse(null);
            if (c == null) continue;

            if (data.containsKey("inclus"))
                c.setInclus(Boolean.parseBoolean(data.get("inclus").toString()));
            if (data.containsKey("justification_exclusion"))
                c.setJustificationExclusion(str(data, "justification_exclusion"));
            if (data.containsKey("reference_doc"))
                c.setReferenceDoc(str(data, "reference_doc"));
            if (data.containsKey("statut_impl"))
                c.setStatutImpl(str(data, "statut_impl"));
            if (data.containsKey("responsable"))
                c.setResponsable(str(data, "responsable"));
            if (data.containsKey("echeance") && data.get("echeance") != null)
                c.setEcheance(LocalDate.parse(str(data, "echeance")));

            // Si exclu → statut non_applicable
            if (Boolean.FALSE.equals(c.getInclus()))
                c.setStatutImpl("non_applicable");

            controleRepository.save(c);
            updated++;
        }
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    // ── PUT /api/soa/controles/{id} ───────────────────────────────────────────
    @PutMapping("/controles/{id}")
    public ResponseEntity<?> updateControle(@PathVariable Long id,
                                             @RequestBody Map<String, Object> data,
                                             @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        SoaControle c = controleRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();

        if (data.containsKey("inclus"))
            c.setInclus(Boolean.parseBoolean(data.get("inclus").toString()));
        if (data.containsKey("justification_exclusion"))
            c.setJustificationExclusion(str(data, "justification_exclusion"));
        if (data.containsKey("reference_doc"))
            c.setReferenceDoc(str(data, "reference_doc"));
        if (data.containsKey("statut_impl"))
            c.setStatutImpl(str(data, "statut_impl"));
        if (data.containsKey("responsable"))
            c.setResponsable(str(data, "responsable"));
        if (data.containsKey("echeance") && data.get("echeance") != null)
            c.setEcheance(LocalDate.parse(str(data, "echeance")));

        if (Boolean.FALSE.equals(c.getInclus()))
            c.setStatutImpl("non_applicable");

        return ResponseEntity.ok(controleToMap(controleRepository.save(c)));
    }

    // ── PUT /api/soa/soumettre ────────────────────────────────────────────────
    @PutMapping("/soumettre")
    public ResponseEntity<?> soumettre(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Long orgId = user.getOrganism().getId();
        Soa soa = soaRepository.findByOrganismId(orgId).orElse(null);
        if (soa == null) return ResponseEntity.notFound().build();

        soa.setStatut("soumis_direction");
        soaRepository.save(soa);
        return ResponseEntity.ok(Map.of("statut", "soumis_direction"));
    }

    // ── PUT /api/soa/valider ──────────────────────────────────────────────────
    @PutMapping("/valider")
    public ResponseEntity<?> valider(@RequestBody Map<String, Object> data,
                                     @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_DIRECTION.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé à la direction"));

        Long orgId = user.getOrganism().getId();
        Soa soa = soaRepository.findByOrganismId(orgId).orElse(null);
        if (soa == null) return ResponseEntity.notFound().build();

        String decision = str(data, "decision");
        soa.setStatut("valide".equals(decision) ? "valide" : "rejete");
        soa.setValidePar(user);
        soa.setValideAt(LocalDateTime.now());
        soa.setCommentaireDirection(str(data, "commentaire"));
        soaRepository.save(soa);

        return ResponseEntity.ok(Map.of("statut", soa.getStatut()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String str(Map<String, Object> d, String k) {
        return d.get(k) != null ? d.get(k).toString() : null;
    }

    private SoaControle ctrl(Soa soa, Organism org, String annexe, String annexeLabel,
                              String id, String label, String desc, String norme) {
        return SoaControle.builder()
                .soa(soa).organism(org)
                .annexe(annexe).annexeLabel(annexeLabel)
                .controleId(id).controleLabel(label)
                .description(desc).norme(norme)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════
    // ISO 27001:2022 — 93 contrôles Annexe A
    // ════════════════════════════════════════════════════════════════════════
    private List<SoaControle> buildControlesIso27001(Soa soa, Organism org) {
        List<SoaControle> list = new ArrayList<>();
        String n = "iso27001";

        // ── A.5 Contrôles organisationnels (37 contrôles) ────────────────────
        String a5 = "A.5"; String l5 = "Contrôles organisationnels";
        list.add(ctrl(soa,org,a5,l5,"A.5.1","Politiques de sécurité de l'information","Des politiques doivent être définies, approuvées, publiées et communiquées.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.2","Rôles et responsabilités","Les rôles doivent être définis et attribués.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.3","Séparation des tâches","Les tâches conflictuelles doivent être séparées.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.4","Responsabilités de la direction","La direction doit exiger le respect des politiques.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.5","Contacts avec les autorités","Des contacts avec les autorités doivent être maintenus.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.6","Contacts avec des groupes spéciaux","Des contacts avec des groupes spécialisés.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.7","Intelligence des menaces","Les informations sur les menaces doivent être collectées.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.8","Sécurité dans la gestion de projet","La sécurité doit être intégrée dans les projets.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.9","Inventaire des actifs","Un inventaire des actifs doit être maintenu.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.10","Utilisation acceptable des actifs","Des règles d'utilisation acceptable doivent être établies.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.11","Restitution des actifs","Les actifs doivent être restitués en fin de contrat.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.12","Classification des informations","Les informations doivent être classifiées.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.13","Étiquetage des informations","Un étiquetage des informations doit être développé.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.14","Transfert des informations","Des règles de transfert doivent exister.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.15","Contrôle d'accès","Des règles de contrôle d'accès doivent être établies.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.16","Gestion des identités","Le cycle de vie des identités doit être géré.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.17","Informations d'authentification","L'allocation des informations d'authentification doit être contrôlée.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.18","Droits d'accès","Les droits d'accès doivent être provisionnés et revus.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.19","Sécurité dans les relations fournisseurs","Les exigences de sécurité pour les fournisseurs doivent être définies.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.20","Sécurité dans les accords fournisseurs","Des accords de sécurité avec les fournisseurs doivent exister.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.21","Sécurité dans la chaîne d'approvisionnement","La sécurité de la chaîne d'approvisionnement doit être gérée.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.22","Surveillance des services fournisseurs","Les services des fournisseurs doivent être surveillés et revus.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.23","Sécurité pour les services cloud","Les processus d'acquisition cloud doivent inclure la sécurité.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.24","Planification de la gestion des incidents","La gestion des incidents doit être planifiée.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.25","Évaluation des événements de sécurité","Les événements doivent être évalués et classifiés.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.26","Réponse aux incidents","Les incidents doivent faire l'objet d'une réponse documentée.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.27","Apprentissage tiré des incidents","Les connaissances tirées des incidents doivent être utilisées.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.28","Collecte de preuves","Des procédures de collecte de preuves doivent exister.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.29","Sécurité pendant une perturbation","La sécurité doit être maintenue en cas de perturbation.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.30","Préparation TIC pour la continuité","La continuité des TIC doit être planifiée.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.31","Exigences légales et réglementaires","Les exigences légales doivent être identifiées et documentées.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.32","Droits de propriété intellectuelle","Des procédures de protection de la PI doivent exister.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.33","Protection des enregistrements","Les enregistrements doivent être protégés.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.34","Confidentialité et protection des DCP","Les exigences de confidentialité doivent être définies.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.35","Revue indépendante de la sécurité","Des revues indépendantes doivent être effectuées.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.36","Conformité aux politiques","La conformité aux politiques doit être revue régulièrement.",n));
        list.add(ctrl(soa,org,a5,l5,"A.5.37","Procédures d'exploitation documentées","Les procédures d'exploitation doivent être documentées.",n));

        // ── A.6 Contrôles des personnes (8 contrôles) ────────────────────────
        String a6 = "A.6"; String l6 = "Contrôles des personnes";
        list.add(ctrl(soa,org,a6,l6,"A.6.1","Contrôle préalable à l'embauche","Les vérifications d'antécédents doivent être effectuées.",n));
        list.add(ctrl(soa,org,a6,l6,"A.6.2","Conditions d'embauche","Les obligations contractuelles doivent inclure la sécurité.",n));
        list.add(ctrl(soa,org,a6,l6,"A.6.3","Sensibilisation et formation","Tous les employés doivent être formés à la sécurité.",n));
        list.add(ctrl(soa,org,a6,l6,"A.6.4","Processus disciplinaire","Un processus disciplinaire formalisé doit exister.",n));
        list.add(ctrl(soa,org,a6,l6,"A.6.5","Responsabilités après fin de contrat","Les responsabilités après fin de contrat doivent être définies.",n));
        list.add(ctrl(soa,org,a6,l6,"A.6.6","Accords de confidentialité","Les accords de confidentialité doivent être signés.",n));
        list.add(ctrl(soa,org,a6,l6,"A.6.7","Travail à distance","Des mesures pour le travail à distance doivent être mises en place.",n));
        list.add(ctrl(soa,org,a6,l6,"A.6.8","Rapports sur les événements","Les événements de sécurité doivent être signalés.",n));

        // ── A.7 Contrôles physiques (14 contrôles) ───────────────────────────
        String a7 = "A.7"; String l7 = "Contrôles physiques";
        list.add(ctrl(soa,org,a7,l7,"A.7.1","Périmètres de sécurité physique","Des périmètres de sécurité physique doivent être définis.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.2","Entrée physique","Les zones sécurisées doivent être protégées par des contrôles d'entrée.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.3","Sécurisation des bureaux","Les bureaux et locaux doivent être sécurisés.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.4","Surveillance de la sécurité physique","Les locaux doivent être surveillés.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.5","Protection contre les menaces physiques","Des protections contre les menaces physiques doivent exister.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.6","Travail dans les zones sécurisées","Des procédures pour travailler dans les zones sécurisées doivent exister.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.7","Bureau propre et écran vide","Des politiques de bureau propre et écran vide doivent être appliquées.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.8","Emplacement et protection des équipements","Les équipements doivent être situés et protégés.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.9","Sécurité des actifs hors des locaux","Les actifs utilisés hors des locaux doivent être protégés.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.10","Supports de stockage","Les supports doivent être gérés tout au long de leur cycle de vie.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.11","Services support","Les services support doivent être protégés.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.12","Sécurité du câblage","Les câbles d'alimentation et de télécommunication doivent être protégés.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.13","Maintenance des équipements","Les équipements doivent être maintenus correctement.",n));
        list.add(ctrl(soa,org,a7,l7,"A.7.14","Mise au rebut ou réutilisation sécurisée","Les équipements doivent être vérifiés avant mise au rebut.",n));

        // ── A.8 Contrôles technologiques (34 contrôles) ──────────────────────
        String a8 = "A.8"; String l8 = "Contrôles technologiques";
        list.add(ctrl(soa,org,a8,l8,"A.8.1","Terminaux utilisateurs","Les informations sur les terminaux doivent être protégées.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.2","Droits d'accès privilégiés","L'attribution des droits d'accès privilégiés doit être contrôlée.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.3","Restriction d'accès aux informations","L'accès aux informations doit être restreint.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.4","Accès au code source","L'accès aux codes source doit être géré.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.5","Authentification sécurisée","Des technologies d'authentification sécurisée doivent être mises en œuvre.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.6","Gestion de la capacité","L'utilisation des ressources doit être surveillée.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.7","Protection contre les logiciels malveillants","Des protections contre les malwares doivent exister.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.8","Gestion des vulnérabilités techniques","Les vulnérabilités doivent être identifiées et corrigées.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.9","Gestion de la configuration","Les configurations doivent être établies et maintenues.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.10","Suppression des informations","Les informations doivent être effacées de manière sécurisée.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.11","Masquage des données","Le masquage des données doit être utilisé.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.12","Prévention des fuites de données","Des mesures de prévention des fuites doivent être appliquées.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.13","Sauvegarde des informations","Des copies de sauvegarde doivent être effectuées régulièrement.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.14","Redondance","Les moyens de traitement doivent être redondants.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.15","Journalisation","Des journaux d'événements doivent être produits et protégés.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.16","Surveillance","Les réseaux et systèmes doivent être surveillés.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.17","Synchronisation des horloges","Les horloges des systèmes doivent être synchronisées.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.18","Programmes utilitaires privilégiés","L'utilisation des utilitaires doit être contrôlée.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.19","Installation de logiciels","L'installation de logiciels doit être contrôlée.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.20","Sécurité des réseaux","Les réseaux doivent être gérés et contrôlés.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.21","Sécurité des services réseau","Les mécanismes de sécurité des services réseau doivent être identifiés.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.22","Filtrage web","L'accès aux sites web externes doit être géré.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.23","Utilisation de la cryptographie","Des règles d'utilisation de la cryptographie doivent être définies.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.24","Gestion des clés cryptographiques","Les clés cryptographiques doivent être gérées.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.25","Développement sécurisé","Des règles de développement sécurisé doivent être établies.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.26","Exigences de sécurité des applications","Les exigences de sécurité doivent être identifiées.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.27","Architecture sécurisée","Les principes d'ingénierie sécurisée doivent être établis.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.28","Codage sécurisé","Les principes de codage sécurisé doivent être appliqués.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.29","Tests de sécurité","Les tests de sécurité doivent être définis.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.30","Développement externalisé","Le développement externalisé doit être supervisé.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.31","Séparation des environnements","Les environnements de dev/test/prod doivent être séparés.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.32","Gestion des changements","Les changements doivent suivre des procédures formelles.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.33","Informations de test","Les informations de test doivent être sélectionnées et protégées.",n));
        list.add(ctrl(soa,org,a8,l8,"A.8.34","Protection en audit","Les systèmes ne doivent pas être perturbés lors des audits.",n));

        return list;
    }

    // ════════════════════════════════════════════════════════════════════════
    // ISO 27701 — Annexes selon rôle organisme
    // ════════════════════════════════════════════════════════════════════════
    private List<SoaControle> buildControlesIso27701(Soa soa, Organism org, String role) {
    List<SoaControle> list = new ArrayList<>();
    String n = "iso27701";

    boolean isRT  = "responsable".equals(role) || "les_deux".equals(role);
    boolean isST  = "sous_traitant".equals(role) || "les_deux".equals(role);

    // ── Annexe A.1 — Responsables de traitement (RT) ─────────────────────
    if (isRT) {
        String a1 = "A.1"; String l1 = "Responsables de traitement de DCP";

        // A.1.2 — Conditions de collecte et de traitement
        list.add(ctrl(soa,org,a1,l1,"A.1.2.2","Identifier et documenter la finalité","Documenter les finalités du traitement.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.2.3","Identifier le fondement juridique","Identifier la base légale pour chaque traitement.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.2.4","Déterminer quand et comment le consentement doit être obtenu","Documenter quand et comment le consentement est obtenu.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.2.5","Obtenir et enregistrer le consentement","Garantir un consentement valide et documenté.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.2.6","Étude de l'impact sur la vie privée","Effectuer une analyse d'impact si nécessaire (DPIA).",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.2.7","Contrats conclus avec les sous-traitants de DCP","S'assurer que les contrats avec sous-traitants incluent la protection des DCP.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.2.8","Responsable conjoint de traitement","Formaliser les responsabilités des responsables conjoints.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.2.9","Enregistrements liés au traitement des DCP","Maintenir un registre des activités de traitement.",n));

        // A.1.3 — Obligations vis-à-vis des personnes concernées
        list.add(ctrl(soa,org,a1,l1,"A.1.3.2","Identifier les obligations vis-à-vis des personnes concernées et y satisfaire","Assurer l'information, l'accès, la rectification et la suppression des DCP.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.3","Déterminer les informations destinées aux personnes concernées","Permettre aux personnes de comprendre comment leurs données sont utilisées.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.4","Fournir des informations aux personnes concernées","Fournir toutes les informations requises par le RGPD.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.5","Fournir un mécanisme permettant de modifier ou de retirer le consentement","Permettre aux personnes de contrôler leurs données.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.6","Fournir un mécanisme permettant de s'opposer au traitement des DCP","Permettre aux personnes de s'opposer au traitement.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.7","Accès, rectification ou suppression","Permettre aux personnes d'exercer leurs droits.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.8","Obligation d'information des tiers","Garantir que les tiers sont informés de leurs obligations.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.9","Fourniture de copies des DCP traitées","Garantir que les personnes peuvent vérifier leurs données.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.10","Gestion des demandes","Documenter et suivre les demandes des personnes concernées.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.3.11","Prise de décision automatisée","Informer et protéger contre les décisions entièrement automatisées.",n));

        // A.1.4 — Protection de la vie privée dès la conception
        list.add(ctrl(soa,org,a1,l1,"A.1.4.2","Limiter la collecte","Collecter uniquement les DCP strictement nécessaires.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.4.3","Limiter le traitement","Limiter l'exposition aux risques liés à un usage excessif des DCP.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.4.4","Exactitude et qualité","Assurer l'exactitude et la mise à jour des DCP.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.4.5","Objectifs de minimisation des DCP","Protéger la vie privée en limitant la collecte aux stricts besoins.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.4.6","Dé-identification et suppression des DCP à la fin du traitement","Limiter les risques liés à la conservation prolongée.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.4.7","Fichiers temporaires","Éviter que des DCP temporaires soient utilisées à d'autres fins.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.4.8","Conservation","Définir des durées de conservation des DCP.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.4.9","Mise au rebut","Détruire les DCP de manière sécurisée.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.4.10","Mesures de transmission des DCP","Sécuriser la transmission des données personnelles.",n));

        // A.1.5 — Partage, transfert et divulgation
        list.add(ctrl(soa,org,a1,l1,"A.1.5.2","Identifier la base du transfert de DCP entre juridictions","Documenter les mécanismes juridiques encadrant les transferts.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.5.3","Pays et organismes internationaux auxquels les DCP peuvent être transférées","Informer des destinations possibles des données.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.5.4","Enregistrements des transferts de DCP","Assurer la traçabilité des transferts de données.",n));
        list.add(ctrl(soa,org,a1,l1,"A.1.5.5","Enregistrements de la divulgation de DCP à des tiers","Garder une visibilité sur les tiers ayant accès aux DCP.",n));
    }

    // ── Annexe A.2 — Sous-traitants (ST) ─────────────────────────────────
    if (isST) {
        String a2 = "A.2"; String l2 = "Sous-traitants de DCP";

        // A.2.2 — Conditions de collecte et de traitement
        list.add(ctrl(soa,org,a2,l2,"A.2.2.2","Contrat client","Encadrer contractuellement les traitements de DCP avec les clients.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.2.3","Finalités de l'organisme","Définir et documenter les finalités des traitements réalisés.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.2.4","Utilisation à des fins de prospection et de publicité","Encadrer l'utilisation des DCP à des fins de prospection.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.2.5","Instruction en infraction","Définir la procédure en cas d'instruction contraire au RGPD.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.2.6","Obligations du client","Définir les responsabilités du client en matière de DCP.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.2.7","Enregistrements liés au traitement des DCP","Maintenir la traçabilité et conformité des traitements de DCP.",n));

        // A.2.3 — Obligations vis-à-vis des personnes concernées
        list.add(ctrl(soa,org,a2,l2,"A.2.3.2","Se conformer aux obligations vis-à-vis des personnes concernées","Respecter les droits des personnes et la conformité RGPD.",n));

        // A.2.4 — Protection de la vie privée dès la conception
        list.add(ctrl(soa,org,a2,l2,"A.2.4.2","Fichiers temporaires","Gestion et suppression sécurisée des fichiers temporaires.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.4.3","Restitution, transfert ou mise au rebut des DCP","Garantir la destruction sécurisée des DCP en fin de traitement.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.4.4","Mesures de transmission des DCP","Garantir la sécurité lors des transmissions de DCP.",n));

        // A.2.5 — Partage, transfert et divulgation
        list.add(ctrl(soa,org,a2,l2,"A.2.5.2","Base du transfert de DCP entre juridictions","Identifier la base légale des transferts internationaux.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.5.3","Pays et organismes internationaux auxquels les DCP peuvent être transférées","Identifier les destinations et garantir un niveau de protection adéquat.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.5.4","Enregistrements de la divulgation de DCP à des tiers","Assurer la traçabilité des divulgations de DCP.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.5.5","Notification des demandes de divulgation de DCP","Notifier et documenter toute demande de divulgation.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.5.6","Divulgations de DCP juridiquement contraignantes","Assurer la conformité lors des divulgations légales.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.5.7","Divulgation des sous-traitants utilisés pour traiter des DCP","Garantir la traçabilité et transparence des sous-traitants.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.5.8","Recrutement d'un sous-traitant pour le traitement de DCP","S'assurer que les sous-traitants respectent le RGPD.",n));
        list.add(ctrl(soa,org,a2,l2,"A.2.5.9","Changement de sous-traitant pour le traitement de DCP","Maintenir la conformité RGPD lors du remplacement d'un sous-traitant.",n));
    }

    // ── Annexe A.3 — RT et ST (les deux) ─────────────────────────────────
    // Toujours inclus quel que soit le rôle
    {
        String a3 = "A.3"; String l3 = "Contrôles communs RT et ST";

        list.add(ctrl(soa,org,a3,l3,"A.3.3","Politiques de sécurité de l'information","Définir les règles et bonnes pratiques pour la sécurité des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.4","Fonctions et responsabilités liées à la sécurité de l'information","Définir les rôles et responsabilités pour la sécurité des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.5","Classification des informations","Catégoriser les informations pour appliquer les mesures de protection adaptées.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.6","Marquage des informations","Marquer les informations pour appliquer des mesures de protection adaptées.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.7","Transfert de l'information","Garantir des transferts d'informations et de DCP sécurisés.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.8","Gestion des identités","S'assurer que seuls les utilisateurs autorisés accèdent aux DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.9","Droits d'accès","Contrôler et documenter les droits d'accès aux DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.10","Sécurité de l'information dans les accords avec les fournisseurs","S'assurer que les fournisseurs respectent les exigences de sécurité.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.11","Planification et préparation de la gestion des incidents","Se préparer à détecter et répondre aux incidents affectant les DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.12","Réponse aux incidents liés à la sécurité de l'information","Traiter efficacement tout incident affectant les DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.13","Exigences légales, statutaires, réglementaires et contractuelles","Assurer la conformité aux obligations légales et réglementaires.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.14","Protection des enregistrements","Protéger les enregistrements contenant des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.15","Revue indépendante de la sécurité de l'information","Évaluer régulièrement l'efficacité des mesures de sécurité.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.16","Conformité aux politiques, règles et normes de sécurité","S'assurer que tous les employés respectent les règles de sécurité.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.17","Sensibilisation, apprentissage et formation à la sécurité","Former le personnel aux bonnes pratiques de protection des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.18","Engagements de confidentialité ou de non-divulgation","Formaliser les obligations de confidentialité des employés.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.19","Bureau propre et écran vide","Réduire le risque d'accès non autorisé aux DCP affichées.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.20","Supports de stockage","Protéger les supports contenant des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.21","Mise au rebut ou recyclage sécurisé du matériel","Détruire ou recycler le matériel contenant des DCP de manière sécurisée.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.22","Terminaux utilisateurs","Sécuriser les terminaux accédant aux DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.23","Authentification sécurisée","Garantir un accès authentifié aux systèmes traitant des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.24","Sauvegarde des informations","Assurer la disponibilité et restauration des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.25","Journalisation","Tracer les accès et opérations sur les DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.26","Utilisation de la cryptographie","Protéger la confidentialité et l'intégrité des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.27","Cycle de vie de développement sécurisé","Intégrer la sécurité dès la conception des applications traitant des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.28","Exigences de sécurité des applications","Définir les exigences minimales de sécurité des applications.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.29","Principes d'ingénierie et d'architecture des systèmes sécurisés","Concevoir des systèmes résilients protégeant les DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.30","Développement externalisé","Encadrer tout développement externalisé traitant des DCP.",n));
        list.add(ctrl(soa,org,a3,l3,"A.3.31","Informations de test","Protéger les données utilisées à des fins de test.",n));
    }

    return list;
}
}