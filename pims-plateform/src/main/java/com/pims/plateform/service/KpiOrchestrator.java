package com.pims.plateform.service;

import com.pims.plateform.dto.KpiResponseDto;
import com.pims.plateform.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class KpiOrchestrator {

    private final SoaKpiCalculator         soaCalculator;
    private final PublicationKpiCalculator publicationCalculator;  // ← ajouté

private final FormationKpiCalculator formationCalculator;

    private static final int DEFAULT_PERIODE_MOIS = 6;

    public KpiResponseDto buildForUser(User user, Integer periodeEvolutionMois) {

        // Sécurité : toujours utiliser l'organisme de l'utilisateur connecté
        Long orgId    = user.getOrganism().getId();
        String orgNom = user.getOrganism().getNom();
        int periode   = (periodeEvolutionMois != null && periodeEvolutionMois > 0
                        && periodeEvolutionMois <= 24)
                        ? periodeEvolutionMois : DEFAULT_PERIODE_MOIS;

        log.info("Calcul KPI pour organisme {} ({}), période {}m", orgId, orgNom, periode);

        var soaKpi = soaCalculator.calculate(orgId, periode);
        var publicationKpi = publicationCalculator.calculate(orgId, periode); // ← ajouté

var formationKpi   = formationCalculator.calculate(orgId, periode);
boolean hasData    = soaKpi.isHasData()
                  || publicationKpi.isHasData()
                  || formationKpi.isHasData();

        return KpiResponseDto.builder()
            .organismId(orgId)
            .organismNom(orgNom)
            .computedAt(LocalDateTime.now())
            .hasData(hasData)
            .messageVide(hasData ? null :
                "Aucun KPI disponible pour votre organisme. " +
                "Les indicateurs apparaîtront dès que des données SOA et publications seront disponibles.")
            .soa(soaKpi)
            .publications(publicationKpi)   // ← ajouté
            .formation(formationKpi)     // ← ajouté
            .build();
    }
}