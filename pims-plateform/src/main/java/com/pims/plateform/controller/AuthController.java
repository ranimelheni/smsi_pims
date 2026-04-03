package com.pims.plateform.controller;

import com.pims.plateform.dto.*;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.UserRepository;
import com.pims.plateform.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // ── POST /api/auth/login ──────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.getEmail() == null || req.getPassword() == null)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email et mot de passe requis"));

        User user = userRepository.findByEmail(req.getEmail()).orElse(null);

        if (user == null || !user.checkPassword(req.getPassword()))
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Identifiants incorrects"));

        if (!user.getIsActive())
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Compte désactivé"));

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken  = jwtUtil.generateToken(String.valueOf(user.getId()));
        String refreshToken = jwtUtil.generateRefreshToken(String.valueOf(user.getId()));

        return ResponseEntity.ok(LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .mustChangePassword(user.getMustChangePassword())
                .user(UserDto.from(user))
                .build());
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails userDetails) {
        Long id   = Long.parseLong(userDetails.getUsername());
        User user = userRepository.findById(id).orElse(null);
        if (user == null)
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Utilisateur non trouvé"));
        return ResponseEntity.ok(UserDto.from(user));
    }

    // ── PUT /api/auth/change-password ─────────────────────────────────────────
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChangePasswordRequest req) {

        Long id   = Long.parseLong(userDetails.getUsername());
        User user = userRepository.findById(id).orElse(null);
        if (user == null)
            return ResponseEntity.status(404)
                    .body(Map.of("error", "Utilisateur non trouvé"));

        if (!user.checkPassword(req.getOldPassword()))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ancien mot de passe incorrect"));

        user.setPassword(req.getNewPassword());
        user.setMustChangePassword(false);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié"));
    }

    // ── GET /api/auth/resolve-organism ────────────────────────────────────────
    @GetMapping("/resolve-organism")
    public ResponseEntity<?> resolveOrganism(@RequestParam String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getOrganism() == null)
            return ResponseEntity.ok(Map.of("organism", (Object) null));
        return ResponseEntity.ok(Map.of("organism", user.getOrganism().getNom()));
    }

    // ── POST /api/auth/refresh ────────────────────────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader("Authorization") String header) {

        if (header == null || !header.startsWith("Bearer "))
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token manquant"));

        String token = header.substring(7);
        if (!jwtUtil.validateToken(token))
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token invalide"));

        String userId   = jwtUtil.extractUserId(token);
        String newToken = jwtUtil.generateToken(userId);
        return ResponseEntity.ok(Map.of("access_token", newToken));
    }
}