package com.pims.plateform.controller;

import com.pims.plateform.dto.EbiosKpiDto;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.UserRepository;
import com.pims.plateform.service.EbiosKpiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ebios/kpi")
@RequiredArgsConstructor
public class EbiosKpiController {

    private final EbiosKpiService kpiService;
    private final UserRepository  userRepo;

    private User getCurrentUser(UserDetails ud) {
        return userRepo.findById(Long.parseLong(ud.getUsername())).orElseThrow();
    }

    @GetMapping("/{analyseId}")
    public ResponseEntity<?> getKpi(
            @PathVariable Long analyseId,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);

        if (!"rssi".equals(user.getRole()))
            return ResponseEntity.status(403)
                .body(Map.of("error", "Accès non autorisé"));

        try {
            EbiosKpiDto kpi = kpiService.getKpi(analyseId, user.getOrganism().getId());
            if (kpi == null) return ResponseEntity.notFound().build();
            return ResponseEntity.ok(kpi);
        } catch (Exception e) {
            log.error("Erreur KPI EBIOS analyseId={} : {}", analyseId, e.getMessage());
            return ResponseEntity.status(500)
                .body(Map.of("error", "Erreur calcul KPI : " + e.getMessage()));
        }
    }
}