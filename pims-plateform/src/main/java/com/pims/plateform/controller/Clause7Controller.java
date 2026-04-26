package com.pims.plateform.controller;

import com.pims.plateform.entity.*;
import com.pims.plateform.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.pims.plateform.service.FileUploadService;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import io.jsonwebtoken.io.IOException;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
@Slf4j
@RestController
@RequestMapping("/api/clause7")
@RequiredArgsConstructor
public class Clause7Controller {

    @Value("${app.upload.base-dir}")
private String baseDir;
    private final EmployeProfilRepository    profilRepo;
    private final FormationSessionRepository sessionRepo;
    private final FormationParticipationRepository partRepo;
    private final CommunicationPublicationRepository pubRepo;
    private final DocumentInfoRepository     docRepo;
    private final FicheProcessusRepository   ficheRepo;
    private final UserRepository             userRepo;
    private final FileUploadService uploadService;


    private static final List<String> ROLES_ADMIN  = List.of("admin_organism","super_admin");
    private static final List<String> ROLES_RSSI   = List.of("rssi","super_admin");
    private static final List<String> ROLES_DIR    = List.of("direction","admin_organism","super_admin");
    private static final List<String> ROLES_PUBLI  = List.of("direction","rssi","admin_organism","super_admin");
private static final List<String> ROLES_PEUT_CREER_SESSION =
    List.of("rssi", "direction", "admin_organism", "super_admin");

private static final List<String> ROLES_PEUT_PUBLIER =
    List.of("rssi", "direction", "admin_organism", "super_admin");
    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    // ════════════════════════════════════════════════════════════════
    // 7.1 — COMPÉTENCES / PROFIL EMPLOYÉ
    // ════════════════════════════════════════════════════════════════

    /** GET mon profil (employé connecté) */
    @GetMapping("/profil/me")
    @Transactional
    public ResponseEntity<?> getMonProfil(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        EmployeProfil profil = profilRepo.findByUserId(user.getId())
            .orElseGet(() -> creerProfil(user));
        return ResponseEntity.ok(profilToMap(profil));
    }

    /** PUT mettre à jour mon profil */
    @PutMapping("/profil/me")
    @Transactional
    public ResponseEntity<?> updateMonProfil(
            @RequestBody Map<String,Object> d,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        EmployeProfil profil = profilRepo.findByUserId(user.getId())
            .orElseGet(() -> creerProfil(user));

        if (d.containsKey("poste"))        profil.setPoste(str(d,"poste"));
        if (d.containsKey("departement"))  profil.setDepartement(str(d,"departement"));
        if (d.containsKey("telephone"))    profil.setTelephone(str(d,"telephone"));
        if (d.containsKey("bio"))          profil.setBio(str(d,"bio"));
        if (d.containsKey("date_entree") && d.get("date_entree") != null)
            profil.setDateEntree(java.time.LocalDate.parse(str(d,"date_entree")));

        return ResponseEntity.ok(profilToMap(profilRepo.save(profil)));
    }

    /** GET liste employés avec scores — pour RSSI */
@GetMapping("/profils/organism")
@Transactional
public ResponseEntity<?> getProfilsOrganism(@AuthenticationPrincipal UserDetails ud) {
    User user = getCurrentUser(ud);

    // RSSI, admin, direction peuvent voir les profils
    if (!List.of("rssi","admin_organism","super_admin","direction")
            .contains(user.getRole()))
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    Long orgId = user.getOrganism().getId();
    List<EmployeProfil> profils = profilRepo.findByOrganismIdWithUser(orgId);
    return ResponseEntity.ok(profils.stream().map(this::profilToMapFull).toList());
}
@GetMapping("/profils/{profilId}")
@Transactional
public ResponseEntity<?> getProfilById(
        @PathVariable Long profilId,
        @AuthenticationPrincipal UserDetails ud) {

    User user = getCurrentUser(ud);
    if (!List.of("rssi","admin_organism","super_admin","direction")
            .contains(user.getRole()))
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    EmployeProfil profil = profilRepo.findById(profilId).orElse(null);
    if (profil == null) return ResponseEntity.notFound().build();

    // Vérifier même organisme
    if (!profil.getOrganism().getId().equals(user.getOrganism().getId()))
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    return ResponseEntity.ok(profilToMapFull(profil));
}
// ── Mapper complet avec cvs et certifs ────────────────────────────────
private Map<String,Object> profilToMapFull(EmployeProfil p) {
    Map<String,Object> m = profilToMap(p); // mapper de base existant

    // Enrichir avec détails CV
    m.put("cvs", p.getCvs().stream().map(cv -> {
        Map<String,Object> c = new LinkedHashMap<>();
        c.put("id",          cv.getId());
        c.put("nom_fichier", cv.getNomFichier());
        c.put("taille",      cv.getTaille());
        c.put("uploaded_at", cv.getUploadedAt() != null
            ? cv.getUploadedAt().toString() : null);
        return c;
    }).toList());

    // Enrichir certifications avec indicateur fichier
    m.put("certifications", p.getCertifications().stream().map(c -> {
        Map<String,Object> cert = new LinkedHashMap<>();
        cert.put("id",              c.getId());
        cert.put("nom",             c.getNom());
        cert.put("organisme",       c.getOrganisme() != null ? c.getOrganisme() : "");
        cert.put("date_obtention",  c.getDateObtention() != null
            ? c.getDateObtention().toString() : "");
        cert.put("date_expiration", c.getDateExpiration() != null
            ? c.getDateExpiration().toString() : "");
        cert.put("nom_fichier",     c.getNomFichier() != null ? c.getNomFichier() : "");
        cert.put("a_fichier",       c.getNomFichier() != null && !c.getNomFichier().isBlank());
        cert.put("profil_user_id",  p.getUser().getId()); // pour le téléchargement
        return cert;
    }).toList());

    return m;
}
// ── Télécharger CV d'un employé — RSSI/admin ─────────────────────────
@GetMapping("/download/cv/{userId}")
@Transactional
public ResponseEntity<?> downloadCv(
        @PathVariable Long userId,
        @AuthenticationPrincipal UserDetails ud) throws IOException {

    User caller = getCurrentUser(ud);

    // Employé télécharge son propre CV OU rssi/admin télécharge celui d'un autre
    boolean isSelf  = caller.getId().equals(userId);
    boolean isAdmin = List.of("rssi","admin_organism","super_admin","direction").contains(caller.getRole());

    if (!isSelf && !isAdmin)
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    EmployeProfil profil = profilRepo.findByUserId(userId).orElse(null);
    if (profil == null || profil.getCvs().isEmpty())
        return ResponseEntity.notFound().build();

    // Vérifier même organisme
    if (!profil.getOrganism().getId().equals(caller.getOrganism().getId()))
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    EmployeCv cv = profil.getCvs().get(0);
    return servirFichier(cv.getCheminFichier(), cv.getNomFichier());
}

// ── Télécharger certification ─────────────────────────────────────────
@GetMapping("/download/certif/{certifId}")
@Transactional
public ResponseEntity<?> downloadCertif(
        @PathVariable Long certifId,
        @AuthenticationPrincipal UserDetails ud) throws IOException {

    User caller = getCurrentUser(ud);

    // Trouver la certif dans tous les profils de l'organisme
    List<EmployeProfil> profils = profilRepo
        .findByOrganismIdWithUser(caller.getOrganism().getId());

    EmployeCertification certif = null;
    EmployeProfil profilOwner   = null;

    for (EmployeProfil p : profils) {
        for (EmployeCertification c : p.getCertifications()) {
            if (c.getId().equals(certifId)) {
                certif      = c;
                profilOwner = p;
                break;
            }
        }
        if (certif != null) break;
    }

    if (certif == null) return ResponseEntity.notFound().build();

    boolean isSelf  = profilOwner.getUser().getId().equals(caller.getId());
    boolean isAdmin = List.of("rssi","admin_organism","super_admin").contains(caller.getRole());

    if (!isSelf && !isAdmin)
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    if (certif.getNomFichier() == null || certif.getCheminFichier() == null)
        return ResponseEntity.notFound().build();

    return servirFichier(certif.getCheminFichier(), certif.getNomFichier());
}

// ── Télécharger document ──────────────────────────────────────────────
@GetMapping("/download/document/{docId}")
@Transactional
public ResponseEntity<?> downloadDocument(
        @PathVariable Long docId,
        @AuthenticationPrincipal UserDetails ud) throws IOException {

    User caller = getCurrentUser(ud);
    DocumentInfo doc = docRepo.findById(docId).orElse(null);
    if (doc == null) return ResponseEntity.notFound().build();

    // Vérifier même organisme
    if (!doc.getOrganism().getId().equals(caller.getOrganism().getId()))
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    // Déposant voit le sien, RSSI voit tout
    boolean isOwner = doc.getUploadedBy().getId().equals(caller.getId());
    boolean isRssi  = List.of("rssi","super_admin").contains(caller.getRole());

    if (!isOwner && !isRssi)
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    if (doc.getNomFichier() == null || doc.getCheminFichier() == null)
        return ResponseEntity.notFound().build();

    return servirFichier(doc.getCheminFichier(), doc.getNomFichier());
}
private ResponseEntity<?> servirFichier(String chemin, String nomFichier) {

    if (chemin == null) return ResponseEntity.notFound().build();

    Path path = Paths.get(chemin);

    if (!Files.exists(path)) {
        log.warn("Fichier introuvable : {}", chemin);
        return ResponseEntity.notFound().build();
    }

    Path base = Paths.get(baseDir).toAbsolutePath().normalize();
    if (!path.toAbsolutePath().normalize().startsWith(base)) {
        log.warn("Accès interdit : {}", chemin);
        return ResponseEntity.status(403).build();
    }

    byte[] bytes = new byte[0]; // ✅ initialisation par défaut
    try {
        try {
            bytes = Files.readAllBytes(path);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    } catch (IOException e) {
        log.error("Erreur lecture fichier : {}", e.getMessage());
        return ResponseEntity.status(500).body(Map.of("error", "Erreur lecture fichier"));
    }

    String nomSafe = (nomFichier != null)
        ? nomFichier.replaceAll("[^a-zA-Z0-9.\\-_]", "_")
        : "fichier";

    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=\"" + nomSafe + "\"")
        .header("Content-Type", detecterMime(nomSafe))
        .header("X-Content-Type-Options", "nosniff")
        .body(new ByteArrayResource(bytes));
}

private String detecterMime(String nomFichier) {
    if (nomFichier == null) return "application/octet-stream";
    String ext = nomFichier.toLowerCase();
    if (ext.endsWith(".pdf"))                          return "application/pdf";
    if (ext.endsWith(".doc"))                          return "application/msword";
    if (ext.endsWith(".docx"))                         return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    if (ext.endsWith(".xls"))                          return "application/vnd.ms-excel";
    if (ext.endsWith(".xlsx"))                         return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    if (ext.endsWith(".ppt"))                          return "application/vnd.ms-powerpoint";
    if (ext.endsWith(".pptx"))                         return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    if (ext.endsWith(".txt"))                          return "text/plain";
    if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
    if (ext.endsWith(".png"))                          return "image/png";
    if (ext.endsWith(".gif"))                          return "image/gif";
    if (ext.endsWith(".zip"))                          return "application/zip";
    return "application/octet-stream";
}

    /** PUT évaluer un employé — admin_organism */
  
    // ════════════════════════════════════════════════════════════════
    // 7.2 — SENSIBILISATION & FORMATION
    // ════════════════════════════════════════════════════════════════

    /** GET toutes les sessions de l'organisme */
    @GetMapping("/formations")
    @Transactional
    public ResponseEntity<?> getSessions(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism().getId();
        List<FormationSession> sessions = sessionRepo.findByOrganismId(orgId);

        // Pour employé : enrichir avec sa participation
        boolean isEmploye = "employe".equals(user.getRole()) || "pilote_processus".equals(user.getRole());

        return ResponseEntity.ok(sessions.stream().map(s -> {
            Map<String,Object> m = sessionToMap(s);
            if (isEmploye) {
                Optional<FormationParticipation> part =
                    partRepo.findBySessionIdAndEmployeId(s.getId(), user.getId());
                m.put("ma_participation", part.map(this::partToMap).orElse(null));
                m.put("suis_inscrit", part.isPresent());
            }
            // Nb participants
            m.put("nb_inscrits", partRepo.findBySessionId(s.getId()).size());
            return m;
        }).toList());
    }

    /** POST créer une session — direction/admin */
@PostMapping("/formations")
@Transactional
public ResponseEntity<?> createSession(
        @RequestBody Map<String,Object> d,
        @AuthenticationPrincipal UserDetails ud) {
    User user = getCurrentUser(ud);

    if (!ROLES_PEUT_CREER_SESSION.contains(user.getRole()))
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    if (d.get("titre") == null || d.get("titre").toString().isBlank())
        return ResponseEntity.badRequest().body(Map.of("error","Le titre est requis"));

    if (d.get("date_debut") == null)
        return ResponseEntity.badRequest().body(Map.of("error","La date de début est requise"));

    try {
        FormationSession session = FormationSession.builder()
            .organism(user.getOrganism())
            .createdBy(user)
            .titre(str(d, "titre"))
            .description(str(d, "description"))
            .type(d.getOrDefault("type", "formation").toString())
            .mode(d.getOrDefault("mode", "presentiel").toString())
            .dateDebut(LocalDateTime.parse(str(d, "date_debut")))
            .dateFin(d.get("date_fin") != null && !str(d,"date_fin").isBlank()
                ? LocalDateTime.parse(str(d, "date_fin")) : null)
            .lieu(str(d, "lieu"))
            .lienVisio(str(d, "lien_visio"))
            .obligatoire(Boolean.parseBoolean(
                d.getOrDefault("obligatoire", "false").toString()))
            .maxParticipants(d.get("max_participants") != null
                ? Integer.parseInt(d.get("max_participants").toString()) : null)
            .statut("planifie")
            .build();

        return ResponseEntity.status(201).body(sessionToMap(sessionRepo.save(session)));

    } catch (Exception e) {
        log.error("Erreur création session: {}", e.getMessage());
        return ResponseEntity.status(500).body(Map.of("error", "Erreur serveur : " + e.getMessage()));
    }
}

    /** POST s'inscrire à une session — employé */
    @PostMapping("/formations/{sessionId}/inscrire")
    @Transactional
    public ResponseEntity<?> sInscrire(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        FormationSession session = sessionRepo.findById(sessionId).orElse(null);
        if (session == null) return ResponseEntity.notFound().build();

        if (partRepo.findBySessionIdAndEmployeId(sessionId, user.getId()).isPresent())
            return ResponseEntity.badRequest().body(Map.of("error","Déjà inscrit"));

        FormationParticipation part = FormationParticipation.builder()
            .session(session).employe(user).statut("inscrit").build();

        return ResponseEntity.status(201).body(partToMap(partRepo.save(part)));
    }

    /** POST se désinscrire */
    @DeleteMapping("/formations/{sessionId}/desinscrire")
    @Transactional
    public ResponseEntity<?> seDesinscrire(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        partRepo.findBySessionIdAndEmployeId(sessionId, user.getId())
            .ifPresent(partRepo::delete);
        return ResponseEntity.ok(Map.of("message","Désinscription effectuée"));
    }

    /** GET participants d'une session — RSSI/direction */
    @GetMapping("/formations/{sessionId}/participants")
    @Transactional
    public ResponseEntity<?> getParticipants(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()) && !ROLES_DIR.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

        List<FormationParticipation> parts = partRepo.findBySessionId(sessionId);
        return ResponseEntity.ok(parts.stream().map(this::partToMap).toList());
    }

   
    // ════════════════════════════════════════════════════════════════
    // 7.3 — COMMUNICATION
    // ════════════════════════════════════════════════════════════════

    /** GET publications visibles selon rôle */
    @GetMapping("/communications")
    @Transactional
    public ResponseEntity<?> getPublications(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism().getId();

        // Admin/direction/rssi voient tout
        List<CommunicationPublication> pubs;
        if (ROLES_PUBLI.contains(user.getRole())) {
            pubs = pubRepo.findAllByOrganismId(orgId);
        } else {
            pubs = pubRepo.findVisibleForRole(orgId, user.getRole());
        }

        // Marquer celles déjà lues
        Set<Long> luesIds = pubs.stream()
            .flatMap(p -> p.getLectures().stream())
            .filter(l -> l.getUser().getId().equals(user.getId()))
            .map(l -> l.getPublication().getId())
            .collect(Collectors.toSet());

        return ResponseEntity.ok(pubs.stream().map(p -> {
            Map<String,Object> m = pubToMap(p);
            m.put("lu", luesIds.contains(p.getId()));
            return m;
        }).toList());
    }

    /** POST créer une publication */
    @PostMapping("/communications")
@Transactional
public ResponseEntity<?> creerPublication(
        @RequestBody Map<String,Object> d,
        @AuthenticationPrincipal UserDetails ud) {
    User user = getCurrentUser(ud);

    if (!ROLES_PEUT_PUBLIER.contains(user.getRole()))
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    if (d.get("titre") == null || d.get("titre").toString().isBlank())
        return ResponseEntity.badRequest().body(Map.of("error","Le titre est requis"));

    if (d.get("contenu") == null || d.get("contenu").toString().isBlank())
        return ResponseEntity.badRequest().body(Map.of("error","Le contenu est requis"));

    try {
        boolean publieNow = Boolean.parseBoolean(
            d.getOrDefault("est_publie", "false").toString());

        CommunicationPublication pub = CommunicationPublication.builder()
            .organism(user.getOrganism())
            .publiePar(user)
            .titre(str(d, "titre"))
            .contenu(str(d, "contenu"))
            .type(d.getOrDefault("type", "information").toString())
            .priorite(d.getOrDefault("priorite", "normale").toString())
            .cible(d.getOrDefault("cible", "tous").toString())
            .estPublie(publieNow)
            .estEpingle(Boolean.parseBoolean(
                d.getOrDefault("est_epingle", "false").toString()))
            .publieAt(publieNow ? LocalDateTime.now() : null)
            .build();

        return ResponseEntity.status(201).body(pubToMap(pubRepo.save(pub)));

    } catch (Exception e) {
        log.error("Erreur création publication: {}", e.getMessage());
        return ResponseEntity.status(500).body(Map.of("error", "Erreur serveur : " + e.getMessage()));
    }
}
    /** POST publier une communication */
    @PatchMapping("/communications/{id}/publier")
    @Transactional
    public ResponseEntity<?> publier(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_PUBLI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

        CommunicationPublication pub = pubRepo.findById(id).orElse(null);
        if (pub == null) return ResponseEntity.notFound().build();

        pub.setEstPublie(true);
        pub.setPublieAt(LocalDateTime.now());
        return ResponseEntity.ok(pubToMap(pubRepo.save(pub)));
    }

    /** POST marquer comme lu */
    @PostMapping("/communications/{id}/lire")
    @Transactional
    public ResponseEntity<?> marquerLu(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        CommunicationPublication pub = pubRepo.findById(id).orElse(null);
        if (pub == null) return ResponseEntity.notFound().build();

        boolean dejalu = pub.getLectures().stream()
            .anyMatch(l -> l.getUser().getId().equals(user.getId()));
        if (!dejalu) {
            CommunicationLecture lecture = CommunicationLecture.builder()
                .publication(pub).user(user).build();
            pub.getLectures().add(lecture);
            pubRepo.save(pub);
        }
        return ResponseEntity.ok(Map.of("message","Marqué comme lu"));
    }

    // ════════════════════════════════════════════════════════════════
    // 7.4 — INFORMATIONS DOCUMENTÉES
    // ════════════════════════════════════════════════════════════════

    /** GET tous les documents de l'organisme */
 

    /** GET documents d'une fiche — RSSI */
  

    /** POST créer un document */
  

    /** PUT approuver un document — RSSI/admin */
    @PatchMapping("/documents/{id}/approuver")
    @Transactional
    public ResponseEntity<?> approuverDoc(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()) && !ROLES_ADMIN.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

        DocumentInfo doc = docRepo.findById(id).orElse(null);
        if (doc == null) return ResponseEntity.notFound().build();

        doc.setStatut("approuve");
        doc.setApprouvePar(user);
        doc.setApprouveAt(LocalDateTime.now());
        return ResponseEntity.ok(docToMap(docRepo.save(doc)));
    }

    // ════════════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════════════

    private Map<String,Object> profilToMap(EmployeProfil p) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",                  p.getId());
        m.put("user_id",             p.getUser().getId());
        m.put("nom",                 p.getUser().getNom());
        m.put("prenom",              p.getUser().getPrenom());
        m.put("email",               p.getUser().getEmail());
        m.put("role",                p.getUser().getRole());
        m.put("poste",               p.getPoste());
        m.put("departement",         p.getDepartement());
        m.put("telephone",           p.getTelephone());
        m.put("bio",                 p.getBio());
        m.put("date_entree",         p.getDateEntree() != null ? p.getDateEntree().toString() : null);
        m.put("statut_evaluation",   p.getStatutEvaluation());
        m.put("score_global",        p.getScoreGlobal());
        m.put("commentaire_admin",   p.getCommentaireAdmin());
        m.put("evalue_par",          p.getEvaluePar() != null
            ? p.getEvaluePar().getPrenom() + " " + p.getEvaluePar().getNom() : null);
        m.put("evalue_at",           p.getEvalueAt() != null ? p.getEvalueAt().toString() : null);
        m.put("nb_cvs",              p.getCvs().size());
        m.put("nb_certifications",   p.getCertifications().size());
        m.put("certifications",      p.getCertifications().stream().map(c -> Map.of(
            "id",              c.getId(),
            "nom",             c.getNom(),
            "organisme",       c.getOrganisme() != null ? c.getOrganisme() : "",
            "date_obtention",  c.getDateObtention() != null ? c.getDateObtention().toString() : "",
            "date_expiration", c.getDateExpiration() != null ? c.getDateExpiration().toString() : "",
            "nom_fichier",     c.getNomFichier() != null ? c.getNomFichier() : ""
        )).toList());
        m.put("created_at", p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String,Object> sessionToMap(FormationSession s) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",               s.getId());
        m.put("titre",            s.getTitre());
        m.put("description",      s.getDescription());
        m.put("type",             s.getType());
        m.put("mode",             s.getMode());
        m.put("date_debut",       s.getDateDebut().toString());
        m.put("date_fin",         s.getDateFin() != null ? s.getDateFin().toString() : null);
        m.put("lieu",             s.getLieu());
        m.put("lien_visio",       s.getLienVisio());
        m.put("statut",           s.getStatut());
        m.put("obligatoire",      s.getObligatoire());
        m.put("max_participants", s.getMaxParticipants());
        m.put("created_by",       s.getCreatedBy().getPrenom() + " " + s.getCreatedBy().getNom());
        m.put("created_at",       s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String,Object> partToMap(FormationParticipation p) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",                  p.getId());
        m.put("session_id",          p.getSession().getId());
        m.put("employe_id",          p.getEmploye().getId());
        m.put("employe_nom",         p.getEmploye().getNom());
        m.put("employe_prenom",      p.getEmploye().getPrenom());
        m.put("employe_email",       p.getEmploye().getEmail());
        m.put("statut",              p.getStatut());
        m.put("presence_confirmee",  p.getPresenceConfirmee());
        m.put("score_evaluation",    p.getScoreEvaluation());
        m.put("commentaire_rssi",    p.getCommentaireRssi());
        m.put("evalue_par",          p.getEvaluePar() != null
            ? p.getEvaluePar().getPrenom() + " " + p.getEvaluePar().getNom() : null);
        m.put("evalue_at",           p.getEvalueAt() != null ? p.getEvalueAt().toString() : null);
        m.put("inscrit_at",          p.getInscritAt() != null ? p.getInscritAt().toString() : null);
        return m;
    }

    private Map<String,Object> pubToMap(CommunicationPublication p) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id",          p.getId());
        m.put("titre",       p.getTitre());
        m.put("contenu",     p.getContenu());
        m.put("type",        p.getType());
        m.put("priorite",    p.getPriorite());
        m.put("cible",       p.getCible());
        m.put("est_publie",  p.getEstPublie());
        m.put("est_epingle", p.getEstEpingle());
        m.put("publie_at",   p.getPublieAt() != null ? p.getPublieAt().toString() : null);
        m.put("expire_at",   p.getExpireAt() != null ? p.getExpireAt().toString() : null);
        m.put("publie_par",  p.getPubliePar().getPrenom() + " " + p.getPubliePar().getNom());
        m.put("publie_par_role", p.getPubliePar().getRole());
        m.put("nb_lectures", p.getLectures().size());
        m.put("created_at",  p.getCreatedAt() != null ? p.getCreatedAt().toString() : null);
        return m;
    }

    // Remplacer docToMap existant
private Map<String,Object> docToMap(DocumentInfo d) {
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("id",             d.getId());
    m.put("reference",      d.getReference());
    m.put("titre",          d.getTitre());
    m.put("type_document",  d.getTypeDocument());
    m.put("version",        d.getVersion());
    m.put("statut",         d.getStatut());
    m.put("description",    d.getDescription());

    // ← Infos fichier — c'était le champ manquant
    m.put("nom_fichier",    d.getNomFichier());
    m.put("taille",         d.getTaille());
    m.put("a_fichier",      d.getNomFichier() != null && !d.getNomFichier().isBlank());

    m.put("date_creation",  d.getDateCreation()  != null ? d.getDateCreation().toString()  : null);
    m.put("date_revision",  d.getDateRevision()  != null ? d.getDateRevision().toString()  : null);
    m.put("date_expiration",d.getDateExpiration()!= null ? d.getDateExpiration().toString(): null);
    m.put("fiche_id",       d.getFiche() != null ? d.getFiche().getId()       : null);
    m.put("fiche_intitule", d.getFiche() != null ? d.getFiche().getIntitule() : null);
    m.put("uploaded_by",    d.getUploadedBy().getPrenom() + " " + d.getUploadedBy().getNom());
    m.put("uploaded_by_id", d.getUploadedBy().getId());
    m.put("uploaded_by_role", d.getUploadedBy().getRole());
    m.put("approuve_par",   d.getApprouvePar() != null
        ? d.getApprouvePar().getPrenom() + " " + d.getApprouvePar().getNom() : null);
    m.put("approuve_at",    d.getApprouveAt() != null ? d.getApprouveAt().toString() : null);
    m.put("created_at",     d.getCreatedAt()  != null ? d.getCreatedAt().toString()  : null);
    return m;
}

    private String str(Map<String,Object> d, String k) {
        return d.get(k) != null ? d.get(k).toString() : null;
    }

    private EmployeProfil creerProfil(User user) {
        EmployeProfil p = EmployeProfil.builder()
            .user(user).organism(user.getOrganism()).build();
        return profilRepo.save(p);
    }
   // ════════════════════════════════════════════════════════════════
// CERTIFICATIONS — endpoints dédiés
// ════════════════════════════════════════════════════════════════

@PostMapping("/profil/me/certifications")
@Transactional
public ResponseEntity<?> ajouterCertification(
        @RequestBody Map<String, Object> d,
        @AuthenticationPrincipal UserDetails ud) {

    User user = getCurrentUser(ud);

    if (d.get("nom") == null || d.get("nom").toString().isBlank())
        return ResponseEntity.badRequest().body(Map.of("error", "Le nom est requis"));

    EmployeProfil profil = profilRepo.findByUserId(user.getId())
            .orElseGet(() -> creerProfil(user));

    EmployeCertification certif = EmployeCertification.builder()
            .profil(profil)
            .nom(str(d, "nom"))
            .organisme(str(d, "organisme"))
            .dateObtention(d.get("date_obtention") != null && !str(d, "date_obtention").isBlank()
                    ? java.time.LocalDate.parse(str(d, "date_obtention")) : null)
            .dateExpiration(d.get("date_expiration") != null && !str(d, "date_expiration").isBlank()
                    ? java.time.LocalDate.parse(str(d, "date_expiration")) : null)
            .build();

    profil.getCertifications().add(certif);
    EmployeProfil saved = profilRepo.save(profil);
    return ResponseEntity.status(201).body(profilToMap(saved));
}


// ── Correction évaluation : admin_organism uniquement ─────────────────
@PutMapping("/profils/{profilId}/evaluer")
@Transactional
public ResponseEntity<?> evaluerEmploye(
        @PathVariable Long profilId,
        @RequestBody Map<String, Object> d,
        @AuthenticationPrincipal UserDetails ud) {

    User user = getCurrentUser(ud);

    // RSSI et direction ne peuvent PAS évaluer
if (!List.of("rssi", "admin_organism", "super_admin").contains(user.getRole()))
            return ResponseEntity.status(403)
                .body(Map.of("error", "Seul l'administrateur de l'organisme peut évaluer les compétences"));

    EmployeProfil profil = profilRepo.findById(profilId).orElse(null);
    if (profil == null) return ResponseEntity.notFound().build();
// empêcher auto-évaluation
if (profil.getUser().getId().equals(user.getId())) {
    return ResponseEntity.badRequest().body(Map.of("error", "Auto-évaluation interdite"));
}

// empêcher évaluation de la direction
if ("direction".equals(profil.getUser().getRole())) {
    return ResponseEntity.badRequest().body(Map.of("error", "La direction ne peut pas être évaluée"));
}
    // Vérifier même organisme
    if (!profil.getOrganism().getId().equals(user.getOrganism().getId()))
        return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

    if (d.get("score_global") == null)
        return ResponseEntity.badRequest().body(Map.of("error", "Le score est requis"));

    profil.setScoreGlobal(new java.math.BigDecimal(str(d, "score_global")));
    profil.setCommentaireAdmin(str(d, "commentaire_admin"));
    profil.setStatutEvaluation("evalue");
    profil.setEvaluePar(user);
    profil.setEvalueAt(LocalDateTime.now());

    return ResponseEntity.ok(profilToMap(profilRepo.save(profil)));
}

// ── Documents : direction + rssi seulement ────────────────────────────
private static final List<String> ROLES_DOCUMENTS =
        List.of("rssi", "direction", "super_admin");



@GetMapping("/documents/fiche/{ficheId}")
@Transactional
public ResponseEntity<?> getDocumentsByFiche(
        @PathVariable Long ficheId,
        @AuthenticationPrincipal UserDetails ud) {
    User user = getCurrentUser(ud);

    if (!ROLES_DOCUMENTS.contains(user.getRole()))
        return ResponseEntity.status(403)
                .body(Map.of("error", "Accès réservé à la direction et au RSSI"));

    return ResponseEntity.ok(
            docRepo.findByFicheId(ficheId).stream().map(this::docToMap).toList());
}

@PostMapping("/documents")
@Transactional
public ResponseEntity<?> creerDocument(
        @RequestBody Map<String, Object> d,
        @AuthenticationPrincipal UserDetails ud) {
    User user = getCurrentUser(ud);

    if (!ROLES_DOCUMENTS.contains(user.getRole()))
        return ResponseEntity.status(403)
                .body(Map.of("error", "Accès réservé à la direction et au RSSI"));

    if (d.get("titre") == null || d.get("titre").toString().isBlank())
        return ResponseEntity.badRequest().body(Map.of("error", "Le titre est requis"));

    FicheProcessus fiche = d.get("fiche_id") != null
            ? ficheRepo.findById(Long.parseLong(str(d, "fiche_id"))).orElse(null)
            : null;

    DocumentInfo doc = DocumentInfo.builder()
            .organism(user.getOrganism())
            .fiche(fiche)
            .uploadedBy(user)
            .reference(str(d, "reference"))
            .titre(str(d, "titre"))
            .typeDocument(d.getOrDefault("type_document", "procedure").toString())
            .version(d.getOrDefault("version", "v1.0").toString())
            .description(str(d, "description"))
            .nomFichier(str(d, "nom_fichier"))
            .statut("brouillon")
            .dateCreation(java.time.LocalDate.now())
            .dateRevision(d.get("date_revision") != null && !str(d, "date_revision").isBlank()
                    ? java.time.LocalDate.parse(str(d, "date_revision")) : null)
            .dateExpiration(d.get("date_expiration") != null && !str(d, "date_expiration").isBlank()
                    ? java.time.LocalDate.parse(str(d, "date_expiration")) : null)
            .build();

    return ResponseEntity.status(201).body(docToMap(docRepo.save(doc)));

}
// ════════════════════════════════════════════════════════════════
// UPLOAD CV
// ════════════════════════════════════════════════════════════════
@PostMapping(value = "/profil/me/cv",
             consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Transactional
public ResponseEntity<?> uploadCv(
        @RequestParam("fichier") MultipartFile fichier,
        @AuthenticationPrincipal UserDetails ud) {

    User user = getCurrentUser(ud);
    EmployeProfil profil = profilRepo.findByUserId(user.getId())
            .orElseGet(() -> creerProfil(user));

    FileUploadService.UploadResult result;
    try {
        result = uploadService.upload(fichier, "cv", user.getOrganism().getId());
    } catch (SecurityException e) {
        log.warn("Upload CV refusé user={} : {}", user.getId(), e.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
        log.error("Erreur upload CV : {}", e.getMessage());
        return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de l'upload"));
    }

    EmployeCv cv = EmployeCv.builder()
            .profil(profil)
            .nomFichier(result.nomOriginal())
            .cheminFichier(result.cheminComplet())
            .taille(result.taille())
            .build();

    // Garder uniquement le dernier CV (remplacer l'ancien)
    if (!profil.getCvs().isEmpty()) {
        EmployeCv ancien = profil.getCvs().get(0);
        uploadService.delete(ancien.getCheminFichier());
        profil.getCvs().clear();
    }
    profil.getCvs().add(cv);

    return ResponseEntity.status(201).body(profilToMap(profilRepo.save(profil)));
}

@DeleteMapping("/profil/me/cv")
@Transactional
public ResponseEntity<?> supprimerCv(@AuthenticationPrincipal UserDetails ud) {
    User user   = getCurrentUser(ud);
    EmployeProfil profil = profilRepo.findByUserId(user.getId()).orElse(null);
    if (profil == null) return ResponseEntity.notFound().build();

    profil.getCvs().forEach(cv -> uploadService.delete(cv.getCheminFichier()));
    profil.getCvs().clear();
    return ResponseEntity.ok(profilToMap(profilRepo.save(profil)));
}

// ════════════════════════════════════════════════════════════════
// UPLOAD CERTIFICATION
// ════════════════════════════════════════════════════════════════
@PostMapping(value = "/profil/me/certifications",
             consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Transactional
public ResponseEntity<?> ajouterCertification(
        @RequestParam("nom")                          String nom,
        @RequestParam(value = "organisme",    required = false) String organisme,
        @RequestParam(value = "date_obtention",required = false) String dateObtention,
        @RequestParam(value = "date_expiration",required = false) String dateExpiration,
        @RequestParam(value = "fichier",      required = false) MultipartFile fichier,
        @AuthenticationPrincipal UserDetails ud) {

    if (nom == null || nom.isBlank())
        return ResponseEntity.badRequest().body(Map.of("error", "Le nom est requis"));

    User user = getCurrentUser(ud);
    EmployeProfil profil = profilRepo.findByUserId(user.getId())
            .orElseGet(() -> creerProfil(user));

    String cheminFichier = null;
    String nomFichier    = null;

    if (fichier != null && !fichier.isEmpty()) {
        FileUploadService.UploadResult result;
        try {
            result = uploadService.upload(fichier, "certif", user.getOrganism().getId());
            cheminFichier = result.cheminComplet();
            nomFichier    = result.nomOriginal();
        } catch (SecurityException e) {
            log.warn("Upload certif refusé user={} : {}", user.getId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur lors de l'upload"));
        }
    }

    EmployeCertification certif = EmployeCertification.builder()
            .profil(profil)
            .nom(nom)
            .organisme(organisme)
            .dateObtention(dateObtention != null && !dateObtention.isBlank()
                ? java.time.LocalDate.parse(dateObtention) : null)
            .dateExpiration(dateExpiration != null && !dateExpiration.isBlank()
                ? java.time.LocalDate.parse(dateExpiration) : null)
            .cheminFichier(cheminFichier)
            .nomFichier(nomFichier)
            .build();

    profil.getCertifications().add(certif);
    return ResponseEntity.status(201).body(profilToMap(profilRepo.save(profil)));
}

@DeleteMapping("/profil/me/certifications/{certifId}")
@Transactional
public ResponseEntity<?> supprimerCertification(
        @PathVariable Long certifId,
        @AuthenticationPrincipal UserDetails ud) {

    User user   = getCurrentUser(ud);
    EmployeProfil profil = profilRepo.findByUserId(user.getId()).orElse(null);
    if (profil == null) return ResponseEntity.notFound().build();

    profil.getCertifications().stream()
        .filter(c -> c.getId().equals(certifId))
        .findFirst()
        .ifPresent(c -> uploadService.delete(c.getCheminFichier()));

    profil.getCertifications().removeIf(c -> c.getId().equals(certifId));
    return ResponseEntity.ok(profilToMap(profilRepo.save(profil)));
}
// ════════════════════════════════════════════════════════════════
// UPLOAD DOCUMENT INFO
// Accessible : rssi, direction, dpo, admin_organism, employe
// Voir tous  : rssi uniquement (pour approbation)
// ════════════════════════════════════════════════════════════════
private static final List<String> ROLES_PEUT_DEPOSER_DOC =
    List.of("rssi", "direction", "dpo", "admin_organism", "employe", "super_admin", "pilote_processus");



// GET documents : chacun voit les siens, rssi voit tout
@GetMapping("/documents")
@Transactional
public ResponseEntity<?> getDocuments(@AuthenticationPrincipal UserDetails ud) {
    User user = getCurrentUser(ud);

    if (!ROLES_PEUT_DEPOSER_DOC.contains(user.getRole()))
        return ResponseEntity.status(403).body(Map.of("error","Accès non autorisé"));

    List<DocumentInfo> docs;
    if (List.of("rssi","super_admin").contains(user.getRole())) {
        // RSSI voit tout l'organisme
        docs = docRepo.findByOrganismId(user.getOrganism().getId());
    } else {
        // Autres rôles voient seulement leurs documents
        docs = docRepo.findByOrganismIdAndUploadedById(
            user.getOrganism().getId(), user.getId());
    }

    return ResponseEntity.ok(docs.stream().map(this::docToMap).toList());
}
@PostMapping(value = "/documents",
             consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@Transactional
public ResponseEntity<?> creerDocument(
        @RequestParam("titre")                           String titre,
        @RequestParam(value = "reference",    required = false) String reference,
        @RequestParam(value = "type_document",required = false) String typeDocument,
        @RequestParam(value = "version",      required = false) String version,
        @RequestParam(value = "description",  required = false) String description,
        @RequestParam(value = "fiche_id",     required = false) Long ficheId,
        @RequestParam(value = "date_revision",required = false) String dateRevision,
        @RequestParam(value = "date_expiration",required = false) String dateExpiration,
        @RequestParam(value = "fichier",      required = false) MultipartFile fichier,
        @AuthenticationPrincipal UserDetails ud) {

    User user = getCurrentUser(ud);

    if (!ROLES_PEUT_DEPOSER_DOC.contains(user.getRole()))
        return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));

    if (titre == null || titre.isBlank())
        return ResponseEntity.badRequest().body(Map.of("error", "Le titre est requis"));

    String cheminFichier = null;
    String nomFichier    = null;

    if (fichier != null && !fichier.isEmpty()) {
        FileUploadService.UploadResult result;
        try {
            result = uploadService.upload(fichier, "document", user.getOrganism().getId());
            cheminFichier = result.cheminComplet();
            nomFichier    = result.nomOriginal();
        } catch (SecurityException e) {
            log.warn("Upload doc refusé user={} : {}", user.getId(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur upload"));
        }
    }

    FicheProcessus fiche = ficheId != null
        ? ficheRepo.findById(ficheId).orElse(null) : null;

    DocumentInfo doc = DocumentInfo.builder()
            .organism(user.getOrganism())
            .fiche(fiche)
            .uploadedBy(user)
            .reference(reference)
            .titre(titre)
            .typeDocument(typeDocument != null ? typeDocument : "procedure")
            .version(version != null ? version : "v1.0")
            .description(description)
            .nomFichier(nomFichier)
            .cheminFichier(cheminFichier)
            .taille(fichier != null ? fichier.getSize() : null)
            .statut("brouillon")
            .dateCreation(java.time.LocalDate.now())
            .dateRevision(dateRevision != null && !dateRevision.isBlank()
                ? java.time.LocalDate.parse(dateRevision) : null)
            .dateExpiration(dateExpiration != null && !dateExpiration.isBlank()
                ? java.time.LocalDate.parse(dateExpiration) : null)
            .build();

    return ResponseEntity.status(201).body(docToMap(docRepo.save(doc)));
}
// ── Évaluation présence : RSSI uniquement (direction = lecture seule) ──
@PutMapping("/formations/participation/{partId}/evaluer")
@Transactional
public ResponseEntity<?> evaluerPresence(
        @PathVariable Long partId,
        @RequestBody Map<String, Object> d,
        @AuthenticationPrincipal UserDetails ud) {

    User user = getCurrentUser(ud);

    // Direction exclue de l'évaluation
    if (!List.of("rssi", "super_admin").contains(user.getRole()))
        return ResponseEntity.status(403)
                .body(Map.of("error", "L'évaluation des présences est réservée au RSSI"));

    FormationParticipation part = partRepo.findById(partId).orElse(null);
    if (part == null) return ResponseEntity.notFound().build();

    if (d.containsKey("statut"))
        part.setStatut(str(d, "statut"));
    if (d.containsKey("presence_confirmee"))
        part.setPresenceConfirmee(
            Boolean.parseBoolean(d.get("presence_confirmee").toString()));
    if (d.containsKey("score_evaluation") && d.get("score_evaluation") != null)
        part.setScoreEvaluation(new BigDecimal(str(d, "score_evaluation")));
    if (d.containsKey("commentaire_rssi"))
        part.setCommentaireRssi(str(d, "commentaire_rssi"));

    part.setEvaluePar(user);
    part.setEvalueAt(LocalDateTime.now());

    return ResponseEntity.ok(partToMap(partRepo.save(part)));
}


}