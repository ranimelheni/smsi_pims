package com.pims.plateform.controller;

import com.pims.plateform.dto.KpiDashboardDto;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.UserRepository;
import com.pims.plateform.service.KpiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService     kpiService;
    private final UserRepository userRepo;

    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    // ── GET /api/kpi/dashboard ────────────────────────────────────────────
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestParam(defaultValue = "true") boolean cache,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);

        if (user.getOrganism() == null)
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Aucun organisme associé"));

        try {
            KpiDashboardDto dashboard = kpiService.buildDashboard(user, cache);
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("Erreur construction KPI dashboard : ", e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Erreur calcul KPI : " + e.getMessage()));
        }
    }

    // ── POST /api/kpi/cache/refresh ───────────────────────────────────────
    @PostMapping("/cache/refresh")
    public ResponseEntity<?> refreshCache(
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);

        if (!java.util.List.of("rssi","super_admin","admin_organism")
                .contains(user.getRole()))
            return ResponseEntity.status(403)
                .body(Map.of("error", "Accès non autorisé"));

        try {
            kpiService.buildDashboard(user, false); // force recalcul
            return ResponseEntity.ok(Map.of("message", "Cache KPI rafraîchi"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", e.getMessage()));
        }
    }
}