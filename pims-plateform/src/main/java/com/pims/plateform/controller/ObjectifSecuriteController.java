package com.pims.plateform.controller;

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
@RequestMapping("/api/objectifs-securite")
@RequiredArgsConstructor
public class ObjectifSecuriteController {

    private final ObjectifSecuriteRepository objectifRepository;
    private final UserRepository             userRepository;

    private static final List<String> ROLES_RSSI = List.of("rssi", "super_admin");

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    private Map<String, Object> toMap(ObjectifSecurite o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",               o.getId());
        m.put("titre",            o.getTitre());
        m.put("description",      o.getDescription());
        m.put("lien_politique",   o.getLienPolitique());
        m.put("responsable",      o.getResponsable());
        m.put("ressources",       o.getRessources());
        m.put("echeance",         o.getEcheance()  != null ? o.getEcheance().toString()  : null);
        m.put("moyen_evaluation", o.getMoyenEvaluation());
        m.put("statut",           o.getStatut());
        m.put("avancement",       o.getAvancement());
        m.put("commentaire",      o.getCommentaire());
        m.put("created_at",       o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        m.put("updated_at",       o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : null);
        return m;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism().getId();
        List<ObjectifSecurite> list = objectifRepository.findByOrganismIdOrderByEcheanceAsc(orgId);

        long atteint    = objectifRepository.countByOrganismIdAndStatut(orgId, "atteint");
        long enCours    = objectifRepository.countByOrganismIdAndStatut(orgId, "en_cours");
        long planifie   = objectifRepository.countByOrganismIdAndStatut(orgId, "planifie");
        long nonAtteint = objectifRepository.countByOrganismIdAndStatut(orgId, "non_atteint");

        return ResponseEntity.ok(Map.of(
            "items", list.stream().map(this::toMap).toList(),
            "stats", Map.of(
                "total",      list.size(),
                "atteint",    atteint,
                "en_cours",   enCours,
                "planifie",   planifie,
                "non_atteint",nonAtteint
            )
        ));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Organism org = user.getOrganism();
        ObjectifSecurite o = ObjectifSecurite.builder()
                .organism(org)
                .titre(str(data, "titre"))
                .description(str(data, "description"))
                .lienPolitique(str(data, "lien_politique"))
                .responsable(str(data, "responsable"))
                .ressources(str(data, "ressources"))
                .moyenEvaluation(str(data, "moyen_evaluation"))
                .statut(data.getOrDefault("statut","planifie").toString())
                .avancement(parseIntOrDefault(data.get("avancement"), 0))
                .commentaire(str(data, "commentaire"))
                .build();

        if (data.get("echeance") != null)
            o.setEcheance(LocalDate.parse(str(data, "echeance")));

        return ResponseEntity.status(201).body(toMap(objectifRepository.save(o)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        ObjectifSecurite o = objectifRepository.findById(id).orElse(null);
        if (o == null) return ResponseEntity.notFound().build();

        if (data.containsKey("titre"))            o.setTitre(str(data, "titre"));
        if (data.containsKey("description"))      o.setDescription(str(data, "description"));
        if (data.containsKey("lien_politique"))   o.setLienPolitique(str(data, "lien_politique"));
        if (data.containsKey("responsable"))      o.setResponsable(str(data, "responsable"));
        if (data.containsKey("ressources"))       o.setRessources(str(data, "ressources"));
        if (data.containsKey("moyen_evaluation")) o.setMoyenEvaluation(str(data, "moyen_evaluation"));
        if (data.containsKey("statut"))           o.setStatut(str(data, "statut"));
        if (data.containsKey("avancement"))       o.setAvancement(parseIntOrDefault(data.get("avancement"), 0));
        if (data.containsKey("commentaire"))      o.setCommentaire(str(data, "commentaire"));
        if (data.containsKey("echeance") && data.get("echeance") != null)
            o.setEcheance(LocalDate.parse(str(data, "echeance")));

        return ResponseEntity.ok(toMap(objectifRepository.save(o)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));
        objectifRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Supprimé"));
    }

    private String str(Map<String, Object> d, String k) { return d.get(k) != null ? d.get(k).toString() : null; }
    private int parseIntOrDefault(Object v, int def) { try { return Integer.parseInt(v.toString()); } catch (Exception e) { return def; } }
}