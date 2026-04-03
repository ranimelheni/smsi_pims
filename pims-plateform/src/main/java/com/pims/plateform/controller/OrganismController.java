package com.pims.plateform.controller;

import com.pims.plateform.dto.OrganismDto;
import com.pims.plateform.entity.AuditType;
import com.pims.plateform.entity.Organism;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.OrganismRepository;
import com.pims.plateform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organisms")
@RequiredArgsConstructor
public class OrganismController {

    private final OrganismRepository organismRepository;
    private final UserRepository     userRepository;

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    // ← helper avec COUNT
    private OrganismDto toDto(Organism o) {
        long nb = organismRepository.countUsersByOrganismId(o.getId());
        return OrganismDto.from(o, nb);
    }

    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        List<Organism> list;

        if ("super_admin".equals(user.getRole())) {
            list = organismRepository.findAllByOrderByCreatedAtDesc();
        } else if (user.getOrganism() != null) {
            list = List.of(user.getOrganism());
        } else {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Aucun organisme associé"));
        }

        return ResponseEntity.ok(list.stream().map(this::toDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user    = getCurrentUser(ud);
        Organism org = organismRepository.findById(id).orElse(null);
        if (org == null) return ResponseEntity.notFound().build();

        if (!"super_admin".equals(user.getRole()) &&
            (user.getOrganism() == null || !user.getOrganism().getId().equals(id)))
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès non autorisé"));

        return ResponseEntity.ok(toDto(org));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!"super_admin".equals(user.getRole()))
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès non autorisé"));

        if (data.get("nom") == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nom requis"));

        Organism org = Organism.builder()
                .nom((String) data.get("nom"))
                .secteur((String) data.get("secteur"))
                .typeOrg((String) data.get("type_org"))
                .auditType(
    data.get("audit_type") != null
        ? AuditType.valueOf(((String) data.get("audit_type")).toUpperCase())
        : AuditType.ISO_27001
)
                .siret((String) data.get("siret"))
                .adresse((String) data.get("adresse"))
                .ville((String) data.get("ville"))
                .pays((String) data.getOrDefault("pays", "France"))
                .emailContact((String) data.get("email_contact"))
                .telephone((String) data.get("telephone"))
                .siteWeb((String) data.get("site_web"))
                .taille((String) data.get("taille"))
                .description((String) data.get("description"))
                .isActive(true)
                .dateAudit(data.get("date_audit") != null
                        ? LocalDate.parse((String) data.get("date_audit")) : null)
                .build();

        return ResponseEntity.status(201)
                .body(toDto(organismRepository.save(org)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!"super_admin".equals(user.getRole()))
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès non autorisé"));

        Organism org = organismRepository.findById(id).orElse(null);
        if (org == null) return ResponseEntity.notFound().build();

        if (data.containsKey("nom"))           org.setNom((String) data.get("nom"));
        if (data.containsKey("secteur"))       org.setSecteur((String) data.get("secteur"));
        if (data.containsKey("type_org"))      org.setTypeOrg((String) data.get("type_org"));
        if (data.containsKey("audit_type"))    org.setAuditType(AuditType.valueOf(((String) data.get("audit_type")).toUpperCase()));
        if (data.containsKey("siret"))         org.setSiret((String) data.get("siret"));
        if (data.containsKey("adresse"))       org.setAdresse((String) data.get("adresse"));
        if (data.containsKey("ville"))         org.setVille((String) data.get("ville"));
        if (data.containsKey("pays"))          org.setPays((String) data.get("pays"));
        if (data.containsKey("email_contact")) org.setEmailContact((String) data.get("email_contact"));
        if (data.containsKey("telephone"))     org.setTelephone((String) data.get("telephone"));
        if (data.containsKey("site_web"))      org.setSiteWeb((String) data.get("site_web"));
        if (data.containsKey("taille"))        org.setTaille((String) data.get("taille"));
        if (data.containsKey("description"))   org.setDescription((String) data.get("description"));
        if (data.containsKey("is_active"))     org.setIsActive((Boolean) data.get("is_active"));
        if (data.containsKey("date_audit") && data.get("date_audit") != null)
            org.setDateAudit(LocalDate.parse((String) data.get("date_audit")));

        return ResponseEntity.ok(toDto(organismRepository.save(org)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        if (!"super_admin".equals(user.getRole()))
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Accès non autorisé"));

        Organism org = organismRepository.findById(id).orElse(null);
        if (org == null) return ResponseEntity.notFound().build();

        org.setIsActive(false);
        organismRepository.save(org);
        return ResponseEntity.ok(Map.of("message", "Organisme désactivé"));
    }
}