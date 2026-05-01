package com.pims.plateform.controller;

import com.pims.plateform.dto.KpiResponseDto;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.UserRepository;
import com.pims.plateform.service.KpiOrchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/kpi")   // ✅ IMPORTANT : cohérent avec Angular
@RequiredArgsConstructor
public class KpiController {

    private final KpiOrchestrator kpiOrchestrator;
    private final UserRepository userRepo;

    // rôles autorisés (format NORMALISÉ sans ROLE_)
    private static final List<String> ROLES_AUTORISES = List.of(
            "rssi",
            "direction",
            "auditeur",
            "super_admin",
            "admin_organism"
    );

    private User getCurrentUser(UserDetails ud) {
        Long userId = Long.parseLong(ud.getUsername());

        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable: " + userId));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestParam(defaultValue = "6") Integer periode,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);

        // ✅ NORMALISATION ROBUSTE DU ROLE
        String role = user.getRole();
        if (role != null) {
            role = role.replace("ROLE_", "").toLowerCase();
        }

        log.info("KPI ACCESS - userId={}, role={}, orgId={}",
                user.getId(),
                role,
                user.getOrganism() != null ? user.getOrganism().getId() : null
        );

        // ❌ accès refusé si rôle non autorisé
        if (role == null || !ROLES_AUTORISES.contains(role)) {
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "error", "Accès non autorisé aux KPI",
                            "role", role
                    ));
        }

        // ❌ utilisateur sans organisme
        if (user.getOrganism() == null) {
            return ResponseEntity.status(400)
                    .body(Map.of(
                            "error", "Aucun organisme associé à votre compte"
                    ));
        }

        try {
            KpiResponseDto response =
                    kpiOrchestrator.buildForUser(user, periode);

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            log.error("Erreur KPI dashboard org={} : {}",
                    user.getOrganism().getId(),
                    e.getMessage(),
                    e
            );

            return ResponseEntity.ok(
                    KpiResponseDto.builder()
                            .organismId(user.getOrganism().getId())
                            .organismNom(user.getOrganism().getNom())
                            .hasData(false)
                            .messageVide("Erreur temporaire de calcul des KPI")
                            .build()
            );
        }
    }
}