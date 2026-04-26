package com.pims.plateform.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KpiDto {
    private String  code;
    private String  label;
    private String  icon;
    private double  valeur;        // 0-100
    private Integer numerateur;
    private Integer denominateur;
    private String  unite;         // % | docs | ...
    private String  tendance;      // hausse | baisse | stable
    private double  tendanceDelta; // différence vs J-7
    private String  couleur;       // green | amber | red
    private boolean visible;       // selon rôle
    
}