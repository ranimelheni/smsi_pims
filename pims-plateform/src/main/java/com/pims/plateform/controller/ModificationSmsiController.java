package com.pims.plateform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.entity.*;
import com.pims.plateform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/modifications-smsi")
@RequiredArgsConstructor
public class ModificationSmsiController {

    private final ModificationSmsiRepository modifRepository;
    private final UserRepository             userRepository;
    private final ObjectMapper               objectMapper = new ObjectMapper();

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
        if (val == null) return List.of();
        try { return objectMapper.readValue(val, Object.class); }
        catch (Exception e) { return List.of(); }
    }

    private Map<String, Object> toMap(ModificationSmsi m) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id",                m.getId());
        r.put("titre",             m.getTitre());
        r.put("description",       m.getDescription());
        r.put("type_modification", m.getTypeModification());
        r.put("impacts",           m.getImpacts());
        r.put("actions",           fromJson(m.getActions()));
        r.put("statut",            m.getStatut());
        r.put("declare_by",        m.getDeclareBy() != null
            ? m.getDeclareBy().getPrenom() + " " + m.getDeclareBy().getNom() : null);
        r.put("created_at",        m.getCreatedAt() != null ? m.getCreatedAt().toString() : null);
        r.put("updated_at",        m.getUpdatedAt() != null ? m.getUpdatedAt().toString() : null);
        return r;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        Long orgId = user.getOrganism().getId();
        return ResponseEntity.ok(modifRepository
                .findByOrganismIdOrderByCreatedAtDesc(orgId)
                .stream().map(this::toMap).toList());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        Organism org = user.getOrganism();
        ModificationSmsi m = ModificationSmsi.builder()
                .organism(org)
                .titre(str(data, "titre"))
                .description(str(data, "description"))
                .typeModification(str(data, "type_modification"))
                .impacts(str(data, "impacts"))
                .actions(toJson(data.get("actions")))
                .statut(data.getOrDefault("statut","en_analyse").toString())
                .declareBy(user)
                .build();

        return ResponseEntity.status(201).body(toMap(modifRepository.save(m)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));

        ModificationSmsi m = modifRepository.findById(id).orElse(null);
        if (m == null) return ResponseEntity.notFound().build();

        if (data.containsKey("titre"))             m.setTitre(str(data, "titre"));
        if (data.containsKey("description"))       m.setDescription(str(data, "description"));
        if (data.containsKey("type_modification")) m.setTypeModification(str(data, "type_modification"));
        if (data.containsKey("impacts"))           m.setImpacts(str(data, "impacts"));
        if (data.containsKey("actions"))           m.setActions(toJson(data.get("actions")));
        if (data.containsKey("statut"))            m.setStatut(str(data, "statut"));

        return ResponseEntity.ok(toMap(modifRepository.save(m)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!ROLES_RSSI.contains(user.getRole()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au RSSI"));
        modifRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Supprimé"));
    }

    private String str(Map<String, Object> d, String k) { return d.get(k) != null ? d.get(k).toString() : null; }
}