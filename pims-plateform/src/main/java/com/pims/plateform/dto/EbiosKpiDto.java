package com.pims.plateform.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EbiosKpiDto {

    private Long    analyseId;
    private String  analyseTitre;
    private String  analyseStatut;

    // Comptages généraux (pour les tuiles)
    private int nbValeursMetier;
    private int nbBiensSupport;
    private int nbEvenements;
    private int nbSourcesRisque;
    private int nbScenariosStrat;
    private int nbScenariosOp;
    private int nbMesures;
    private int nbResiduels;

    // KPI 1 — Couverture des risques
    private double kpiCouverture;       // % scénarios traités
    private int    couvertureNum;
    private int    couvertureDen;
    private int    couvertureAcceptes;

    // KPI 2 — Risque résiduel
    private double kpiNiveauResiduel;   // moyenne 1-4
    private int    residuelTotal;
    private int    residuelEleve;
    private int    residuelMoyen;
    private int    residuelFaible;

    // KPI 3 — Mesures de sécurité
    private double kpiMesures;          // % réalisées
    private int    mesuresNum;
    private int    mesuresDen;
    private int    mesuresEnCours;
    private int    mesuresPlanifiees;

    // Niveau global calculé côté service
    private String niveauGlobal;        // Faible | Moyen | Élevé | Critique
    private String couleurGlobale;      // green | amber | red
    private int    scoreCompletion;     // 0-100 %
}