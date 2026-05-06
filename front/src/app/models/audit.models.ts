// audit.models.ts
export interface AuditSession {
  id: number;
  titre: string;
  norme: string;
  statut: string;
  version: string;
  date_debut: string;
  date_fin: string | null;
  auditeur: string;
  created_at: string;
}

export interface AuditContexte {
  // Organisme
  organism_nom: string;
  type_audit: string | null;

  // Statut global direction (clause 5)
  statut_direction: string | null;
  politique_diffusion: any[] | null;

  // Clause 4.1 & 4.3
  perimetre_smsi: string | null;
  perimetre_pims: string | null;
  enjeux_externes: any[];
  enjeux_internes: any[];
  parties_interessees: any[];
  sites_concernes: string | null;
  activites_exclues: string | null;
  justification_exclusions: string | null;
  interfaces_dependances: string | null;

  // Clause 4.4
  engagement_direction: string | null;
  politique_securite: string | null;
  objectifs_smsi: any[];
  ressources_humaines: any[];
  ressources_logicielles: any[];
  ressources_materielles: any[];
  procedures: any[];
  outils_protection: any[];
  version_clause4: string | null;
  statut_clause4: string | null;
  date_revue: string | null;

  // Clause 5 — Validations direction
  validation_enjeux_externes: string | null;
  validation_enjeux_internes: string | null;
  validation_parties: string | null;
  validation_perimetre: string | null;
  validation_ressources: string | null;
  politique_securite_contenu: string | null;
  objectifs_securite_metier: any[];

  // Objectifs de sécurité RSSI
  objectifs_securite_rssi: any[] | null;

  // Modifications SMSI
  modifications_smsi: any[] | null;

  // Méthodologie
  methode_risque: string | null;
  methode_statut: string | null;
  methode_commentaire_direction: string | null;
  methode_valide_by: string | null;
  methode_valide_at: string | null;
  methode_justification: string | null;
  echelle_probabilite: number | null;
  echelle_impact: number | null;
  seuil_acceptable: number | null;
  seuil_eleve: number | null;
  labels_probabilite: string[];
  labels_impact: string[];

  // SOA
  soa_statut: string | null;
  soa_version: string | null;
  soa_nb_controles: number;
  soa_nb_implemente: number;
  soa_taux: number;
}

export interface AuditEvaluation {
  id: number | null;
  clause_code: string;
  clause_titre: string;
  clause_desc: string;
  parent_code: string | null;
  statut: string;
  justification: string | null;
  action_planifiee: string | null;
  priorite: string;
  echeance: string | null;
  responsable: string | null;
}

export interface AuditKpi {
  session_id: number;
  total_clauses_evaluees: number;
  nb_conforme: number;
  nb_non_conforme: number;
  nb_partiel: number;
  nb_planifie: number;
  nb_en_cours: number;
  nb_non_applicable: number;
  nb_non_evalue: number;
  taux_conformite_globale: number;
  taux_conformite_partielle: number;
  par_clause: ClauseKpi[];
}

export interface ClauseKpi {
  clause_principale: string;
  total: number;
  nb_conforme: number;
  nb_non_conforme: number;
  nb_partiel: number;
  taux_clause: number;
}