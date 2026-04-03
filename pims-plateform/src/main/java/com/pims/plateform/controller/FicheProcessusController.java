package com.pims.plateform.controller;

import com.pims.plateform.dto.FicheProcessusDto;
import com.pims.plateform.dto.FicheProcessusRequest;
import com.pims.plateform.entity.AuditType;
import com.pims.plateform.entity.FicheProcessus;
import com.pims.plateform.entity.Organism;
import com.pims.plateform.entity.User;
import com.pims.plateform.repository.FicheProcessusRepository;
import com.pims.plateform.repository.OrganismRepository;
import com.pims.plateform.repository.UserRepository;
import com.pims.plateform.service.FicheProcessusService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fiches")
@RequiredArgsConstructor
public class FicheProcessusController {

    private final FicheProcessusRepository ficheRepository;
    private final OrganismRepository organismRepository;
    private final UserRepository userRepository;
    private final FicheProcessusService mapper;

    // ── Rôles autorisés ────────────────────────────────────────────────────
    private static final List<String> FICHE_ROLES =
            List.of("pilote_processus", "dpo", "rssi", "admin_organism", "super_admin");

 

    // ── Helpers ────────────────────────────────────────────────────────────

    private User getCurrentUser(UserDetails ud) {
        return userRepository.findById(Long.parseLong(ud.getUsername()))
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

  

    private ResponseEntity<?> forbidden(String msg) {
        return ResponseEntity.status(403).body(Map.of("error", msg));
    }

    // ── GET /api/fiches/mine ───────────────────────────────────────────────
    /**
     * Retourne la fiche du pilote connecté.
     * Si elle n'existe pas encore, elle est créée automatiquement.
     * DPO et RSSI reçoivent la liste des fiches en attente de leur action.
     */
    @GetMapping("/mine")
    public ResponseEntity<?> getMine(@AuthenticationPrincipal UserDetails ud) {
        User user = getCurrentUser(ud);
        String role = user.getRole();

        if (!FICHE_ROLES.contains(role))
            return forbidden("Accès non autorisé — rôle : " + role);

        return switch (role) {
            case "pilote_processus" -> handlePilote(user);
            case "dpo"              -> handleDpo(user);
            case "rssi"             -> handleRssi(user);
            default                 -> forbidden("Accès non autorisé");
        };
    }

    private ResponseEntity<?> handlePilote(User user) {
        if (user.getOrganism() == null)
            return ResponseEntity.status(400)
                    .body(Map.of("error", "Aucun organisme associé au pilote"));

        Long orgId = user.getOrganism().getId();

        FicheProcessus fiche = ficheRepository
                .findByOrganismIdAndPiloteId(orgId, user.getId())
                .orElseGet(() -> createFicheForPilote(user, orgId));

        fiche.setAuditType(fiche.getOrganism().getAuditType());
ficheRepository.save(fiche);

return ResponseEntity.ok(mapper.toDto(fiche));
    }

    private FicheProcessus createFicheForPilote(User user, Long orgId) {
        Organism org = organismRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organisme introuvable"));

        FicheProcessus fiche = FicheProcessus.builder()
                .organism(org)
                .pilote(user)
                .auditType(org.getAuditType())
                .intitule(user.getProcessusPilote() != null
                        ? user.getProcessusPilote() : "Nouveau processus")
                .finalite("")
                .statut("brouillon")
                .build();

        return ficheRepository.save(fiche);
    }

    private ResponseEntity<?> handleDpo(User user) {
        if (user.getOrganism() == null)
            return ResponseEntity.status(400).body(Map.of("error", "Aucun organisme associé"));

        List<FicheProcessusDto> fiches = ficheRepository
                .findByOrganismIdAndStatut(user.getOrganism().getId(), "soumis_dpo")
                .stream().map(mapper::toDto).toList();

        return ResponseEntity.ok(fiches);
    }

    private ResponseEntity<?> handleRssi(User user) {
        if (user.getOrganism() == null)
            return ResponseEntity.status(400).body(Map.of("error", "Aucun organisme associé"));

        List<FicheProcessusDto> fiches = ficheRepository
                .findByOrganismIdAndStatutIn(
                        user.getOrganism().getId(),
                        List.of("soumis_rssi", "complete_dpo"))
                .stream().map(mapper::toDto).toList();

        return ResponseEntity.ok(fiches);
    }

    // ── GET /api/fiches/organism/{organismId} ──────────────────────────────
    @GetMapping("/organism/{organismId}")
    public ResponseEntity<?> getByOrganism(
            @PathVariable Long organismId,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);

        boolean isSuperAdmin = "super_admin".equals(user.getRole());
        boolean isSameOrg = user.getOrganism() != null
                && user.getOrganism().getId().equals(organismId);

        if (!isSuperAdmin && !isSameOrg)
            return forbidden("Accès non autorisé");

        return ResponseEntity.ok(
                ficheRepository.findByOrganismId(organismId)
                        .stream().map(mapper::toDto).toList());
    }

    // ── GET /api/fiches/{id} ───────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        FicheProcessus fiche = ficheRepository.findById(id).orElse(null);
        if (fiche == null) return ResponseEntity.notFound().build();
        if (!hasReadAccess(user, fiche)) return forbidden("Accès non autorisé");

        return ResponseEntity.ok(mapper.toDto(fiche));
    }

    // ── PUT /api/fiches/{id} ───────────────────────────────────────────────
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody FicheProcessusRequest req,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        FicheProcessus fiche = ficheRepository.findById(id).orElse(null);
        if (fiche == null) return ResponseEntity.notFound().build();
        if (!hasWriteAccess(user, fiche))
            return forbidden("Modification non autorisée");

        applyUpdates(fiche, req);
        return ResponseEntity.ok(mapper.toDto(ficheRepository.save(fiche)));
    }

    private void applyUpdates(FicheProcessus fiche, FicheProcessusRequest req) {
        // Champs texte simples
        if (req.getIntitule()            != null) fiche.setIntitule(req.getIntitule());
        if (req.getCode()                != null) fiche.setCode(req.getCode());
        if (req.getTypeProcessus()       != null) fiche.setTypeProcessus(req.getTypeProcessus());
        if (req.getDomaine()             != null) fiche.setDomaine(req.getDomaine());
        if (req.getActivites()           != null) fiche.setActivites(req.getActivites());
        if (req.getVersion()             != null) fiche.setVersion(req.getVersion());
        if (req.getFinalite()            != null) fiche.setFinalite(req.getFinalite());
        if (req.getContraintesInternes() != null) fiche.setContraintesInternes(req.getContraintesInternes());
        if (req.getContraintesTemporelles() != null) fiche.setContraintesTemporelles(req.getContraintesTemporelles());
        if (req.getContraintegTechniques() != null) fiche.setContraintegTechniques(req.getContraintegTechniques());
        if (req.getRisqueDominant()      != null) fiche.setRisqueDominant(req.getRisqueDominant());
        if (req.getNoteMax()             != null) fiche.setNoteMax(req.getNoteMax());

        // Champs JSONB — convertir Object → JSON String
        if (req.getBeneficiaires()                  != null) fiche.setBeneficiaires(mapper.toJson(req.getBeneficiaires()));
        if (req.getDeclencheurs()                   != null) fiche.setDeclencheurs(mapper.toJson(req.getDeclencheurs()));
        if (req.getElementsEntree()                 != null) fiche.setElementsEntree(mapper.toJson(req.getElementsEntree()));
        if (req.getElementsSortieIntentionnels()    != null) fiche.setElementsSortieIntentionnels(mapper.toJson(req.getElementsSortieIntentionnels()));
        if (req.getElementsSortieNonIntentionnels() != null) fiche.setElementsSortieNonIntentionnels(mapper.toJson(req.getElementsSortieNonIntentionnels()));
        if (req.getInformationsDocumentees()        != null) fiche.setInformationsDocumentees(mapper.toJson(req.getInformationsDocumentees()));
        if (req.getContraintesReglementaires()      != null) fiche.setContraintesReglementaires(mapper.toJson(req.getContraintesReglementaires()));
        if (req.getActeurs()                        != null) fiche.setActeurs(mapper.toJson(req.getActeurs()));
        if (req.getRessources()                     != null) fiche.setRessources(mapper.toJson(req.getRessources()));
        if (req.getObjectifsKpi()                   != null) fiche.setObjectifsKpi(mapper.toJson(req.getObjectifsKpi()));
        if (req.getMoyensSurveillance()             != null) fiche.setMoyensSurveillance(mapper.toJson(req.getMoyensSurveillance()));
        if (req.getMoyensMesure()                   != null) fiche.setMoyensMesure(mapper.toJson(req.getMoyensMesure()));
        if (req.getInteractions()                   != null) fiche.setInteractions(mapper.toJson(req.getInteractions()));
        if (req.getRisques()                        != null) fiche.setRisques(mapper.toJson(req.getRisques()));
        if (req.getOpportunites()                   != null) fiche.setOpportunites(mapper.toJson(req.getOpportunites()));
        if (req.getDataDpo()                        != null) fiche.setDataDpo(mapper.toJson(req.getDataDpo()));
    }

    // ── PUT /api/fiches/{id}/statut ────────────────────────────────────────
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> updateStatut(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        FicheProcessus fiche = ficheRepository.findById(id).orElse(null);
        if (fiche == null) return ResponseEntity.notFound().build();

        String role   = user.getRole();
        String statut = (String) data.get("statut");

        return switch (role) {
            case "pilote_processus" -> soumettreFiche(user, fiche);
            case "dpo"              -> completeDpo(user, fiche);
            case "rssi"             -> validerFiche(user, fiche, statut, (String) data.get("commentaire"));
            default                 -> forbidden("Action non autorisée");
        };
    }

   private ResponseEntity<?> soumettreFiche(User user, FicheProcessus fiche) {
    if (!user.getId().equals(fiche.getPilote() != null ? fiche.getPilote().getId() : null))
        return forbidden("Accès non autorisé");
    if (!"brouillon".equals(fiche.getStatut()))
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Seule une fiche brouillon peut être soumise"));

    // ── Flow selon type d'audit ─────────────────────────────
    String prochainStatut;
    if (fiche.getAuditType() == AuditType.ISO_27701) {
        // Pour ISO_27701, le pilote soumet → DPO complète → RSSI valide
        prochainStatut = "soumis_dpo";
    } else {
        // Pour ISO_27001, le pilote soumet directement au RSSI
        prochainStatut = "soumis_rssi";
    }

    fiche.setStatut(prochainStatut);
    fiche.setSoumisAt(LocalDateTime.now());
    ficheRepository.save(fiche);

    return ResponseEntity.ok(Map.of("statut", prochainStatut, "message", "Fiche soumise"));
}

private ResponseEntity<?> completeDpo(User user, FicheProcessus fiche) {
    if (user.getOrganism() == null
            || !user.getOrganism().getId().equals(fiche.getOrganism().getId()))
        return forbidden("Accès non autorisé");
    if (!"soumis_dpo".equals(fiche.getStatut()))
        return ResponseEntity.badRequest()
                .body(Map.of("error", "La fiche n'est pas en attente DPO"));

    fiche.setStatut("complete_dpo"); // DPO complète
    fiche.setDpoAt(LocalDateTime.now()); // date de complétion DPO
    ficheRepository.save(fiche);
    return ResponseEntity.ok(Map.of("statut", "complete_dpo", "message", "Section DPO complétée"));
}

private ResponseEntity<?> validerFiche(User user, FicheProcessus fiche,
                                        String statut, String commentaire) {
    if (user.getOrganism() == null
            || !user.getOrganism().getId().equals(fiche.getOrganism().getId()))
        return forbidden("Accès non autorisé");

    // Pour RSSI, les fiches valides pour action :
    // - ISO_27001 : statut = "soumis_rssi"
    // - ISO_27701 : statut = "complete_dpo"
    if (!List.of("soumis_rssi", "complete_dpo").contains(fiche.getStatut()))
        return ResponseEntity.badRequest()
                .body(Map.of("error", "La fiche n'est pas en attente RSSI"));

    if (!List.of("valide", "rejete").contains(statut))
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Statut invalide : utilisez 'valide' ou 'rejete'"));

    fiche.setStatut(statut);
    fiche.setValidePar(user);
    fiche.setValideAt(LocalDateTime.now());
    if ("rejete".equals(statut)) fiche.setCommentaireRejet(commentaire);

    ficheRepository.save(fiche);
    return ResponseEntity.ok(Map.of("statut", statut, "message", "Fiche " + statut));
}
    // ── DELETE /api/fiches/{id} ────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails ud) {

        User user = getCurrentUser(ud);
        if (!List.of("super_admin", "admin_organism").contains(user.getRole()))
            return forbidden("Accès non autorisé");

        FicheProcessus fiche = ficheRepository.findById(id).orElse(null);
        if (fiche == null) return ResponseEntity.notFound().build();

        ficheRepository.delete(fiche);
        return ResponseEntity.ok(Map.of("message", "Fiche supprimée"));
    }
    @PutMapping("/{id}/dpo")
    public ResponseEntity<?> updateDpo(@PathVariable Long id,
                                    @RequestBody Map<String, Object> data,
                                    @AuthenticationPrincipal UserDetails ud) {
        User user  = getCurrentUser(ud);
        FicheProcessus fiche = ficheRepository.findById(id).orElse(null);
        if (fiche == null) return ResponseEntity.notFound().build();

        if (!"dpo".equals(user.getRole()) ||
            !user.getOrganism().getId().equals(fiche.getOrganism().getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Réservé au DPO"));

        if (!List.of("soumis_dpo", "complete_dpo", "rejete").contains(fiche.getStatut()))
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La fiche n'est pas accessible au DPO"));

        Object dataDpo = data.get("data_dpo");
        fiche.setDataDpo(toJsonString(dataDpo));
        ficheRepository.save(fiche);

        return ResponseEntity.ok(Map.of("message", "Section DPO sauvegardée"));
    }

    // ── Accès ──────────────────────────────────────────────────────────────

  private String toJsonString(Object data) {
    try {
        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
    } catch (Exception e) {
        throw new RuntimeException("Erreur conversion JSON", e);
    }
}

    private boolean hasReadAccess(User user, FicheProcessus fiche) {
        return switch (user.getRole()) {
            case "super_admin" -> true;
            case "rssi", "admin_organism", "dpo" ->
                    user.getOrganism() != null
                    && user.getOrganism().getId().equals(fiche.getOrganism().getId());
            case "pilote_processus" ->
                    fiche.getPilote() != null
                    && user.getId().equals(fiche.getPilote().getId());
            default -> false;
        };
    }

    private boolean hasWriteAccess(User user, FicheProcessus fiche) {
        if (!"pilote_processus".equals(user.getRole())) return false;
        return fiche.getPilote() != null
                && user.getId().equals(fiche.getPilote().getId())
                && "brouillon".equals(fiche.getStatut());
    }
    
}