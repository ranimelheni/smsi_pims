package com.pims.plateform.controller;

import com.pims.plateform.dto.UserDto;
import com.pims.plateform.entity.Organism;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.OrganismRepository;
import com.pims.plateform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/actors")
@RequiredArgsConstructor
public class ActorController {

    private final UserRepository     userRepository;
    private final OrganismRepository organismRepository;
    //private final PasswordEncoder    passwordEncoder;

    private static final List<String> ROLES_ALLOWED = List.of(
        "admin_organism", "rssi", "dpo", "iso",
        "auditeur_interne", "auditeur_externe", "pilote_processus",
        "proprietaire_risque", "proprietaire_actif", "responsable_conformite",
         "responsable_qualite", "utilisateur_metier","employe",
        "direction", "comite_securite","membre_equipe_technique"
    );

    private static final Map<String, String> ROLES_LABELS = Map.ofEntries(
        Map.entry("admin_organism",         "Administrateur organisme"),
        Map.entry("rssi",                   "RSSI"),
        Map.entry("dpo",                    "DPO / Délégué protection données"),
        Map.entry("iso",                    "ISO / Responsable SI"),
        Map.entry("auditeur_interne",       "Auditeur interne"),
        Map.entry("auditeur_externe",       "Auditeur externe"),
        Map.entry("pilote_processus",       "Pilote de processus"),
        Map.entry("membre_equipe_technique", "Membre de l'équipe technique"),
        Map.entry("proprietaire_risque",    "Propriétaire des risques"),
        Map.entry("proprietaire_actif",     "Propriétaire des actifs"),
        Map.entry("responsable_conformite", "Responsable conformité"),
        Map.entry("responsable_qualite",    "Responsable qualité"),
        Map.entry("utilisateur_metier",     "Utilisateur métier"),
        Map.entry("employe",                "Employé"),
        Map.entry("direction",              "Direction / DSI"),
        Map.entry("comite_securite",        "Comité sécurité"),
            Map.entry("super_admin",           "Super administrateur")
    );

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    // ── GET /api/actors ───────────────────────────────────────────────────────
@GetMapping
public ResponseEntity<?> getActors(
        @RequestParam(required = false) Long organism_id,
        @AuthenticationPrincipal UserDetails ud) {

    User user = getCurrentUser(ud);
    List<User> actors;

    if ("super_admin".equals(user.getRole())) {
        actors = organism_id != null
                ? userRepository.findByOrganismId(organism_id)
                : userRepository.findByRoleNot("super_admin");
    } else if (user.getOrganism() != null) {
        actors = userRepository.findByOrganismId(user.getOrganism().getId());
    } else {
        return ResponseEntity.status(403)
                .body(Map.of("error", "Aucun organisme associé"));
    }

    return ResponseEntity.ok(actors.stream().map(UserDto::from).toList());
}

    // ── GET /api/actors/roles ─────────────────────────────────────────────────
    @GetMapping("/roles")
    public ResponseEntity<?> getRoles() {
        List<Map<String, String>> roles = ROLES_LABELS.entrySet().stream()
                .map(e -> Map.of("value", e.getKey(), "label", e.getValue()))
                .toList();
        return ResponseEntity.ok(roles);
    }

    // ── POST /api/actors ──────────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);

        if (!List.of("super_admin", "admin_organism").contains(user.getRole()))
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès non autorisé"));

        for (String f : List.of("email", "nom", "prenom", "role", "organism_id")) {
            if (data.get(f) == null)
                return ResponseEntity.badRequest()
                        .body(Map.of("error", f + " est requis"));
        }

        String role = (String) data.get("role");
        if (!ROLES_ALLOWED.contains(role))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rôle invalide"));

        if ("pilote_processus".equals(role) && data.get("processus_pilote") == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Le processus piloté est requis"));

        if (userRepository.existsByEmail((String) data.get("email")))
            return ResponseEntity.status(409)
                    .body(Map.of("error", "Email déjà utilisé"));

        Long orgId = Long.parseLong(data.get("organism_id").toString());
        Organism org = organismRepository.findById(orgId).orElse(null);
        if (org == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Organisme non trouvé"));

        String tempPassword = generateTempPassword();

        User actor = User.builder()
                .email((String) data.get("email"))
                .nom((String) data.get("nom"))
                .prenom((String) data.get("prenom"))
                .role(role)
                .telephone((String) data.get("telephone"))
                .organism(org)
                .isActive(true)
                .mustChangePassword(true)
                .processusPilote((String) data.get("processus_pilote"))
                .build();
        actor.setPassword(tempPassword);
        userRepository.save(actor);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id",               actor.getId());
        result.put("email",            actor.getEmail());
        result.put("nom",              actor.getNom());
        result.put("prenom",           actor.getPrenom());
        result.put("role",             actor.getRole());
        result.put("organism_id",      org.getId());
        result.put("organism",         org.getNom());
        result.put("processus_pilote", actor.getProcessusPilote());
        result.put("temp_password",    tempPassword);
        return ResponseEntity.status(201).body(result);
    }

    // ── PUT /api/actors/{id} ──────────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user  = getCurrentUser(ud);
        if (!List.of("super_admin", "admin_organism").contains(user.getRole()))
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès non autorisé"));

        User actor = userRepository.findById(id).orElse(null);
        if (actor == null) return ResponseEntity.notFound().build();

        if (data.containsKey("nom"))              actor.setNom((String) data.get("nom"));
        if (data.containsKey("prenom"))           actor.setPrenom((String) data.get("prenom"));
        if (data.containsKey("telephone"))        actor.setTelephone((String) data.get("telephone"));
        if (data.containsKey("role"))             actor.setRole((String) data.get("role"));
        if (data.containsKey("is_active"))        actor.setIsActive((Boolean) data.get("is_active"));
        if (data.containsKey("processus_pilote")) actor.setProcessusPilote((String) data.get("processus_pilote"));

        if (data.containsKey("role") && !"pilote_processus".equals(data.get("role")))
            actor.setProcessusPilote(null);

        return ResponseEntity.ok(UserDto.from(userRepository.save(actor)));
    }

    // ── DELETE /api/actors/{id} ───────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user  = getCurrentUser(ud);
        if (!List.of("super_admin", "admin_organism").contains(user.getRole()))
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès non autorisé"));

        User actor = userRepository.findById(id).orElse(null);
        if (actor == null) return ResponseEntity.notFound().build();
        actor.setIsActive(false);
        userRepository.save(actor);
        return ResponseEntity.ok(Map.of("message", "Acteur désactivé"));
    }

    // ── POST /api/actors/{id}/reset-password ──────────────────────────────────
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails ud) {
        User user  = getCurrentUser(ud);
        if (!List.of("super_admin", "admin_organism").contains(user.getRole()))
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès non autorisé"));

        User actor = userRepository.findById(id).orElse(null);
        if (actor == null) return ResponseEntity.notFound().build();

        String temp = generateTempPassword();
        actor.setPassword(temp);
        actor.setMustChangePassword(true);
        userRepository.save(actor);
        return ResponseEntity.ok(Map.of("temp_password", temp));
    }
}