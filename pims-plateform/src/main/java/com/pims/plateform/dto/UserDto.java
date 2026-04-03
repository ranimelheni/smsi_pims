package com.pims.plateform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pims.plateform.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {

    private Long id;
    private String email;
    private String nom;
    private String prenom;
    private String role;
    private String telephone;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("must_change_password")
    private Boolean mustChangePassword;

    @JsonProperty("organism_id")
    private Long organismId;

    private String organism;

    @JsonProperty("processus_pilote")
    private String processusPilote;
    @JsonProperty("responsabilite_technique")
    private String responsabiliteTechnique;


    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("last_login")
    private LocalDateTime lastLogin;

    public static UserDto from(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .role(user.getRole())
                .telephone(user.getTelephone())
                .isActive(user.getIsActive())
                .mustChangePassword(user.getMustChangePassword())
                .organismId(user.getOrganism() != null ? user.getOrganism().getId()  : null)
                .organism(user.getOrganism()  != null ? user.getOrganism().getNom() : null)
                .processusPilote(user.getProcessusPilote())
                .responsabiliteTechnique(user.getResponsabiliteTechnique())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}