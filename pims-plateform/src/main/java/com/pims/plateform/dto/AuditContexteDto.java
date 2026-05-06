package com.pims.plateform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AuditContexteDto {

    // ── Organisme ────────────────────────────────────────────
    @JsonProperty("organism_nom")          private String organismNom;
    @JsonProperty("type_audit")            private String typeAudit;

    // ── Statut global direction (clause 5) ───────────────────
    @JsonProperty("statut_direction")       private String statutDirection;
    @JsonProperty("politique_diffusion")    private Object politiqueDiffusion;

    // ── Clause 4.1 — Enjeux ──────────────────────────────────
    @JsonProperty("perimetre_smsi")        private String perimetreSmsi;
    @JsonProperty("perimetre_pims")        private String perimetrePims;
    @JsonProperty("enjeux_externes")       private Object enjeuxExternes;
    @JsonProperty("enjeux_internes")       private Object enjeuxInternes;

    // ── Clause 4.2 — Parties intéressées ─────────────────────
    @JsonProperty("parties_interessees")   private Object partiesInteressees;

    // ── Clause 4.3 — Périmètre ───────────────────────────────
    @JsonProperty("sites_concernes")       private String sitesConcernes;
    @JsonProperty("activites_exclues")     private String activitesExclues;
    @JsonProperty("justification_exclusions") private String justificationExclusions;
    @JsonProperty("interfaces_dependances")private String interfacesDependances;

    // ── Clause 4.4 — Ressources & Politiques ─────────────────
    @JsonProperty("engagement_direction")  private String engagementDirection;
    @JsonProperty("politique_securite")    private String politiqueSecurite;
    @JsonProperty("objectifs_smsi")        private Object objectifsSmsi;
    @JsonProperty("ressources_humaines")   private Object ressourcesHumaines;
    @JsonProperty("ressources_logicielles")private Object ressourcesLogicielles;
    @JsonProperty("ressources_materielles")private Object ressourcesMaterielles;
    @JsonProperty("procedures")            private Object procedures;
    @JsonProperty("outils_protection")     private Object outilsProtection;
    @JsonProperty("version_clause4")       private String versionClause4;
    @JsonProperty("statut_clause4")        private String statutClause4;
    @JsonProperty("date_revue")            private String dateRevue;

    // ── Clause 5 — Validation direction ──────────────────────
    @JsonProperty("validation_enjeux_externes") private String validationEnjeuxExternes;
    @JsonProperty("validation_enjeux_internes") private String validationEnjeuxInternes;
    @JsonProperty("validation_parties")         private String validationParties;
    @JsonProperty("validation_perimetre")       private String validationPerimetre;
    @JsonProperty("validation_ressources")      private String validationRessources;
    @JsonProperty("politique_securite_contenu") private String politiqueSecuriteContenu;
    @JsonProperty("objectifs_securite_metier")  private Object objectifsSecuriteMetier;

    // ── Objectifs de sécurité RSSI ────────────────────────────
    @JsonProperty("objectifs_securite_rssi")    private Object objectifsSecuriteRssi;

    // ── Gestion des modifications SMSI ───────────────────────
    @JsonProperty("modifications_smsi")         private Object modificationsSmsi;

    // ── Méthodologie risque ───────────────────────────────────
    @JsonProperty("methode_risque")        private String methodeRisque;
    @JsonProperty("methode_statut")        private String methodeStatut;
    @JsonProperty("methode_commentaire_direction") private String methodeCommentaireDirection;
    @JsonProperty("methode_valide_by")     private String methodeValidePar;
    @JsonProperty("methode_valide_at")     private String methodeValideAt;
    @JsonProperty("methode_justification") private String methodeJustification;
    @JsonProperty("echelle_probabilite")   private Integer echelleProbabilite;
    @JsonProperty("echelle_impact")        private Integer echelleImpact;
    @JsonProperty("seuil_acceptable")      private Integer seuilAcceptable;
    @JsonProperty("seuil_eleve")           private Integer seuilEleve;
    @JsonProperty("labels_probabilite")    private Object labelsProbabilite;
    @JsonProperty("labels_impact")         private Object labelsImpact;

    // ── SOA ───────────────────────────────────────────────────
    @JsonProperty("soa_statut")            private String soaStatut;
    @JsonProperty("soa_version")           private String soaVersion;
    @JsonProperty("soa_nb_controles")      private Integer soaNbControles;
    @JsonProperty("soa_nb_implemente")     private Integer soaNbImplemente;
    @JsonProperty("soa_taux")             private Double soaTaux;
}