package com.pims.plateform.dto;

import com.pims.plateform.entity.Organism;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class OrganismDto {

    private Long id;
    private String nom;
    private String secteur;
    private String typeOrg;
    private String auditType;
    private String siret;
    private String adresse;
    private String ville;
    private String pays;
    private String emailContact;
    private String telephone;
    private String siteWeb;
    private String taille;
    private String description;
    private Boolean isActive;
    private LocalDate dateAudit;
    private Long nbActeurs;
    private LocalDateTime createdAt;

    public static OrganismDto from(Organism o, long nbActeurs) {
        return OrganismDto.builder()
                .id(o.getId())
                .nom(o.getNom())
                .secteur(o.getSecteur())
                .typeOrg(o.getTypeOrg())
                .auditType(o.getAuditType() != null ? o.getAuditType().name() : null)
                .siret(o.getSiret())
                .adresse(o.getAdresse())
                .ville(o.getVille())
                .pays(o.getPays())
                .emailContact(o.getEmailContact())
                .telephone(o.getTelephone())
                .siteWeb(o.getSiteWeb())
                .taille(o.getTaille())
                .description(o.getDescription())
                .isActive(o.getIsActive())
                .dateAudit(o.getDateAudit())
                .createdAt(o.getCreatedAt())
                .build();
    }
}