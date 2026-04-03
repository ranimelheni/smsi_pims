package com.pims.plateform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pims.plateform.dto.FicheProcessusDto;
import com.pims.plateform.entity.FicheProcessus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Convertit entre FicheProcessus (entity) et FicheProcessusDto.
 * Gère la sérialisation / désérialisation JSON des champs JSONB.
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class FicheProcessusService {

    private final ObjectMapper objectMapper;

    // ── Entity → DTO ───────────────────────────────────────────────────────
    
    public FicheProcessusDto toDto(FicheProcessus f) {
        return FicheProcessusDto.builder()
                .id(f.getId())
                .organismId(f.getOrganism().getId())
                .auditType(f.getAuditType().name())
                .intitule(f.getIntitule())
                .code(f.getCode())
                .typeProcessus(f.getTypeProcessus())
                .domaine(f.getDomaine())
                .activites(f.getActivites())
                .version(f.getVersion())
                .finalite(f.getFinalite())
                .beneficiaires(fromJson(f.getBeneficiaires()))
                .declencheurs(fromJson(f.getDeclencheurs()))
                .elementsEntree(fromJson(f.getElementsEntree()))
                .elementsSortieIntentionnels(fromJson(f.getElementsSortieIntentionnels()))
                .elementsSortieNonIntentionnels(fromJson(f.getElementsSortieNonIntentionnels()))
                .informationsDocumentees(fromJson(f.getInformationsDocumentees()))
                .contraintesReglementaires(fromJson(f.getContraintesReglementaires()))
                .contraintesInternes(f.getContraintesInternes())
                .contraintesTemporelles(f.getContraintesTemporelles())
                .contraintegTechniques(f.getContraintegTechniques())
                .piloteId(f.getPilote() != null ? f.getPilote().getId() : null)
                .piloteNom(f.getPilote() != null
                        ? f.getPilote().getPrenom() + " " + f.getPilote().getNom() : null)
                .acteurs(fromJson(f.getActeurs()))
                .ressources(fromJson(f.getRessources()))
                .objectifsKpi(fromJson(f.getObjectifsKpi()))
                .moyensSurveillance(fromJson(f.getMoyensSurveillance()))
                .moyensMesure(fromJson(f.getMoyensMesure()))
                .interactions(fromJson(f.getInteractions()))
                .risques(fromJson(f.getRisques()))
                .noteMax(f.getNoteMax())
                .risqueDominant(f.getRisqueDominant())
                .opportunites(fromJson(f.getOpportunites()))
                .dataDpo(fromJson(f.getDataDpo()))
                .statut(f.getStatut())
                .soumisAt(f.getSoumisAt() != null ? f.getSoumisAt().toString() : null)
                .valideAt(f.getValideAt() != null ? f.getValideAt().toString() : null)
                .valideBy(f.getValidePar() != null ? f.getValidePar().getId() : null)
                .commentaireRejet(f.getCommentaireRejet())
                .createdAt(f.getCreatedAt() != null ? f.getCreatedAt().toString() : null)
                .updatedAt(f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : null)
                .build();
    }

    // ── JSON helpers ───────────────────────────────────────────────────────

    /** Convertit un objet Java (List / Map / String brute) → JSON String pour JSONB. */
    public String toJson(Object value) {
        if (value == null) return null;
        if (value instanceof String s) {
            // Déjà du JSON valide ? on le retourne tel quel
            String trimmed = s.trim();
            if (trimmed.startsWith("[") || trimmed.startsWith("{")) return trimmed;
            // Sinon on le sérialise en tant que string JSON
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Erreur sérialisation JSON : {}", e.getMessage());
            return "[]";
        }
    }

    /** Convertit une JSON String stockée en JSONB → Object Java pour le DTO. */
    public Object fromJson(String value) {
        if (value == null || value.isBlank()) return java.util.List.of();
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (Exception e) {
            log.warn("Erreur désérialisation JSON : {}", e.getMessage());
            return java.util.List.of();
        }
    }
}