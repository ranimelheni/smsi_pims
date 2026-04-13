// RegistreTraitement.java
package com.pims.plateform.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registre_traitement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegistreTraitement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organism_id", nullable = false)
    private Organism organism;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fiche_processus_id")
    private FicheProcessus ficheProcessus;

    // Identification
    @Column(nullable = false)  private String service;
    @Column(name = "macro_finalite")  private String macroFinalite;
    @Column(name = "micro_finalites", columnDefinition = "TEXT") private String microFinalites;
    @Column(name = "autres_finalites",columnDefinition = "TEXT") private String autresFinalites;
    @Column(name = "categorie_traitement") private String categorieTraitement;

    // Dates
    @Column(name = "date_creation")    private LocalDate dateCreation;
    @Column(name = "date_mise_a_jour") private LocalDate dateMiseAJour;

    // Base légale
    @Column(name = "base_legale", length = 100) private String baseLegale;

    // Personnes
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories_personnes", columnDefinition = "jsonb")
    @Builder.Default private String categoriesPersonnes = "[]";

    @Column(name = "nombre_personnes") private String nombrePersonnes;

    // Données
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories_donnees", columnDefinition = "jsonb")
    @Builder.Default private String categoriesDonnees = "[]";

    @Column(name = "details_donnees", columnDefinition = "TEXT") private String detailsDonnees;

    @Column(name = "donnees_hautement_personnelles")
    @Builder.Default private Boolean donneesHautementPersonnelles = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "donnees_sensibles", columnDefinition = "jsonb")
    @Builder.Default private String donneesSensibles = "[]";

    // Collecte
    @Column(name = "mode_collecte")            private String modeCollecte;
    @Column(name = "duree_conservation_definie")
    @Builder.Default private Boolean dureeConservationDefinie = false;
    @Column(name = "duree_conservation")       private String dureeConservation;

    // Droits
    @Column(name = "information_personnes")    private String informationPersonnes;
    @Column(name = "mode_consentement")        private String modeConsentement;
    @Builder.Default private Boolean droitAcces         = false;
    @Builder.Default private Boolean droitRectification = false;
    @Builder.Default private Boolean droitOpposition    = false;
    @Builder.Default private Boolean droitPortabilite   = false;
    @Builder.Default private Boolean droitLimitation    = false;
    @Builder.Default private Boolean notificationViolation = false;

    // Conservation & accès
    @Column(name = "gestion_conservation", columnDefinition = "TEXT") private String gestionConservation;
    @Column(name = "acces_donnees",        columnDefinition = "TEXT") private String accesDonnees;

    // Partage
    @Builder.Default private Boolean partageDonnees = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "type_destinataires", columnDefinition = "jsonb")
    @Builder.Default private String typeDestinataires = "[]";

    private String prestataire;
    @Column(name = "contrat_prestataire")      private String contratPrestataire;
    @Column(name = "clause_protection_donnees")
    @Builder.Default private Boolean clauseProtectionDonnees = false;

    // Transferts
    @Column(name = "transfert_hors_ue")
    @Builder.Default private Boolean transfertHorsUE = false;
    @Column(name = "pays_destination")  private String paysDestination;
    @Column(name = "fondement_transfert") private String fondementTransfert;

    // Outils
    @Column(name = "outil_utilise")    private String outilUtilise;
    @Column(name = "description_outil",columnDefinition = "TEXT") private String descriptionOutil;
    @Column(name = "methode_stockage") private String methodeStockage;
    @Column(name = "lieu_stockage")    private String lieuStockage;
    @Column(name = "pays_stockage")    private String paysStockage;

    // Sécurité
    @Column(name = "securite_physique",        columnDefinition = "TEXT") private String securitePhysique;
    @Builder.Default private Boolean authentification = false;
    @Builder.Default private Boolean journalisation   = false;
    @Column(name = "reseau_interne")
    @Builder.Default private Boolean reseauInterne    = false;
    @Builder.Default private Boolean chiffrement      = false;
    @Column(name = "autres_mesures_securite",  columnDefinition = "TEXT") private String autresMesuresSecurite;
    @Column(name = "mesures_a_implementer",    columnDefinition = "TEXT") private String mesuresAImplementer;

    // Responsabilités
    @Column(name = "responsable_traitement") private String responsableTraitement;
    @Column(name = "responsable_conjoint")   private String responsableConjoint;
    @Column(name = "contact_interne")        private String contactInterne;
    @Column(name = "region_responsable")     private String regionResponsable;
    @Column(name = "reference_cnil")         private String referenceCnil;

    // Risques
    @Column(name = "risque_physique")  @Builder.Default private Short risquePhysique  = 0;
    @Column(name = "risque_moral")     @Builder.Default private Short risqueMoral      = 0;
    @Column(name = "risque_materiel")  @Builder.Default private Short risqueMateriel   = 0;
    @Column(name = "note_max")         private Short noteMax;
    @Column(name = "risque_max")       private String risqueMax;
    @Column(name = "domaine_risque_max") private String domaineRisqueMax;
    @Column(name = "explication_risque", columnDefinition = "TEXT") private String explicationRisque;

    // PIA
    @Column(name = "analyse_pia")  @Builder.Default private Boolean analysePia  = false;
    @Column(name = "pia_requis")   @Builder.Default private Boolean piaRequis   = false;
    @Column(name = "pia_statut",   length = 30)
    @Builder.Default private String piaStatut = "non_requis";

    // Statut
    @Column(length = 30)
    @Builder.Default private String statut = "brouillon";

    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}